package com.kubukoz.nixmilk

import cats.effect.Async
import cats.effect.Concurrent
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.effect.implicits._
import cats.implicits._
import io.circe.generic.auto._
import io.circe.literal._
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

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    if (args.headOption.contains("test"))
      IO.println("pong").as(ExitCode.Success)
    else
      BlazeClientBuilder[IO]
        .resource
        .flatMap { implicit c =>
          BlazeServerBuilder[IO]
            .bindHttp(8080, "0.0.0.0")
            .withHttpApp(routes[IO].orNotFound)
            .resource
            .productL(IO.println("Started").toResource)
        }
        .useForever

  def routes[F[_]: Async: Client]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes.of { case GET -> Root / "vscode-extensions" / publisher / name / "latest.zip" =>
      val getExt =
        for {
          v <- latestVersion[F](publisher, name)
          sha256 <- nixPrefetchExtension(publisher, name, v)
        } yield Extension(publisher, name, v, sha256)

        getExt
          .flatMap { ext =>
            Ok(fs2.Stream.eval(src[F].map(replace(_, ext))).through(bytes[F]))
          }
          .map(_.withContentType(`Content-Type`(MediaType.application.zip)))
    }
  }

  // files

  def src[F[_]: Async] =
    fs2
      .io
      .readInputStream(
        Sync[F].delay(getClass.getResourceAsStream("/flake.nix")),
        4096,
      )
      .through(fs2.text.utf8.decode[F])
      .compile
      .string

  def bytes[F[_]: Async]: fs2.Pipe[F, String, Byte] =
    src => {
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

      fs2
        .io
        .readOutputStream[F](4096) { os =>
          src
            .through(fs2.text.utf8.encode[F])
            .through(mkZipSink(os))
            .compile
            .drain
        }
    }

  // nix

  def nixPrefetchExtension[F[_]: Sync](
    publisher: String,
    name: String,
    version: String,
  ): F[String] = Sync[F].blocking {
    import sys.process._

    val url =
      s"https://$publisher.gallery.vsassets.io/_apis/public/gallery/publisher/$publisher/extension/$name/$version/assetbyname/Microsoft.VisualStudio.Services.VSIXPackage"

    s"nix-prefetch-url $url".!!.trim
  }

  // vscode

  def latestVersion[F[_]: Concurrent](
    publisher: String,
    name: String,
  )(
    implicit c: Client[F]
  ) = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._
    import org.http4s.circe.CirceEntityCodec._

    val url = Uri.unsafeFromString(
      s"https://$publisher.gallery.vsassets.io/_apis/public/gallery/publisher/$publisher/extension/$name/latest/assetbyname/Microsoft.VisualStudio.Code.Manifest"
    )

    final case class Manifest(version: String)
    c.expect[Manifest](GET(url)).map(_.version)
  }

  // util

  def replace(template: String, ext: Extension): String = template
    .replace("TEMPLATE_NAME", ext.name)
    .replace("TEMPLATE_PUBLISHER", ext.publisher)
    .replace("TEMPLATE_VERSION", ext.version)
    .replace("TEMPLATE_SHA256", ext.sha256)

}

final case class Extension(publisher: String, name: String, version: String, sha256: String)
