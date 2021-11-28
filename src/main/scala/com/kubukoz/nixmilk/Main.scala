package com.kubukoz.nixmilk

import cats.Monad
import cats.Show
import cats.effect.Async
import cats.effect.Concurrent
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.effect.implicits._
import cats.effect.kernel.Resource
import cats.implicits._
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.RedisCommands
import dev.profunktor.redis4cats.algebra.Getter
import dev.profunktor.redis4cats.algebra.Setter
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.Method._
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import scala.concurrent.duration.FiniteDuration

import concurrent.duration._

object Main extends IOApp:
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] =
    if (args.headOption.contains("test"))
      IO.println("pong").as(ExitCode.Success)
    else
      configR[IO].flatMap { cfg =>
        BlazeClientBuilder[IO]
          .resource
          .flatMap { client =>
            given Client[IO] = client

            redis[IO].flatMap { redis =>
              given RedisCommands[IO, String, String] = redis

              BlazeServerBuilder[IO]
                .bindHttp(cfg.port, "0.0.0.0")
                .withHttpApp(routes[IO](cfg).orNotFound)
                .resource
                .flatTap { server =>
                  IO.println(s"Started nix-milk at ${server.address}").toResource
                }
            }
          }
      }.useForever

end Main

final case class Config(
  port: Int,
  shaCacheTTL: Option[FiniteDuration],
)

def redis[F[_]: Async: Log]: Resource[F, RedisCommands[F, String, String]] = RedisClient[F]
  .from("redis://localhost")
  .flatMap(Redis[F].fromClient(_, RedisCodec.Utf8))

def configR[F[_]: Async: Logger] = {
  import ciris._
  (env("HTTP_PORT").as[Int].default(8080), env("SHA_CACHE_TTL").as[FiniteDuration].option)
    .mapN(Config.apply)
}.resource[F].evalTap(cfg => Logger[F].info(s"Loaded configuration $cfg"))

def getOrFetchCaching[F[_]: Monad: Logger, K: Show, V](
  key: K,
  ttl: Option[FiniteDuration],
)(
  fetch: F[V]
)(
  using
  getter: Getter[F, K, V],
  setter: Setter[F, K, V],
): F[V] = getter.get(key).flatMap {
  case Some(v) => v.pure[F]
  case None =>
    Logger[F].info(s"Didn't find $key in cache, fetching") *>
      fetch.flatTap { result =>
        ttl.fold(setter.set(key, result))(setter.setEx(key, result, _))
      }
}

def routes[F[_]: Async: Client: Logger](
  cfg: Config
)(
  using redis: RedisCommands[F, String, String]
): HttpRoutes[F] =
  val dsl = new Http4sDsl[F] {}
  import dsl._

  HttpRoutes.of {
    case GET -> Root =>
      Ok(
        """<h1>Welcome to nix-milk! Check <a href="https://github.com/kubukoz/nix-milk">README</a> for instructions.</h1>"""
      ).map(_.withContentType(`Content-Type`(MediaType.text.html)))

    case GET -> Root / "vscode-extensions" / publisher / name / "latest.zip" =>
      val getExt =
        for {
          v <- latestVersion[F](publisher, name)
          sha256 <-
            getOrFetchCaching(s"$publisher.$name.$v.sha256", cfg.shaCacheTTL)(
              nixPrefetchExtension(publisher, name, v)
            )
        } yield Extension(publisher, name, v, sha256)

      getExt
        .flatMap { ext =>
          Ok(fs2.Stream.eval(src[F].map(replace(_, ext))).through(bytes[F]))
        }
        .map(_.withContentType(`Content-Type`(MediaType.application.zip)))
  }

// files

def src[F[_]: Async]: F[String] =
  fs2
    .io
    .readInputStream(
      Sync[F].delay(this.getClass.getResourceAsStream("/flake.nix")),
      4096,
    )
    .through(fs2.text.utf8.decode[F])
    .compile
    .string

def bytes[F[_]: Async]: fs2.Pipe[F, String, Byte] =
  def mkZipSink(os: OutputStream) = fs2
    .io
    .writeOutputStream {
      Sync[F]
        .delay(new ZipOutputStream(os))
        .flatTap { zos =>
          Sync[F].delay(zos.putNextEntry(new ZipEntry("result/flake.nix")))
        }
        .widen[OutputStream]
    }

  src =>
    fs2
      .io
      .readOutputStream[F](4096) { os =>
        src
          .through(fs2.text.utf8.encode[F])
          .through(mkZipSink(os))
          .compile
          .drain
      }

// nix

def nixPrefetchExtension[F[_]: Sync](
  publisher: String,
  name: String,
  version: String,
): F[String] =
  import sys.process._

  val url =
    s"https://$publisher.gallery.vsassets.io/_apis/public/gallery/publisher/$publisher/extension/$name/$version/assetbyname/Microsoft.VisualStudio.Services.VSIXPackage"

  Sync[F].blocking(s"nix-prefetch-url $url".!!.trim)

// vscode

def latestVersion[F[_]: Concurrent](
  publisher: String,
  name: String,
)(
  implicit c: Client[F]
): F[String] =
  val dsl = new Http4sClientDsl[F] {}

  import dsl._
  import org.http4s.circe.CirceEntityCodec._

  val url = Uri.unsafeFromString(
    s"https://$publisher.gallery.vsassets.io/_apis/public/gallery/publisher/$publisher/extension/$name/latest/assetbyname/Microsoft.VisualStudio.Code.Manifest"
  )

  final case class Manifest(version: String)
  c.expect[Manifest](GET(url)).map(_.version)

// util

def replace(template: String, ext: Extension): String = template
  .replace("TEMPLATE_NAME", ext.name)
  .replace("TEMPLATE_PUBLISHER", ext.publisher)
  .replace("TEMPLATE_VERSION", ext.version)
  .replace("TEMPLATE_SHA256", ext.sha256)

final case class Extension(publisher: String, name: String, version: String, sha256: String)
