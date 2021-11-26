package com.kubukoz.nixmilk

import cats.effect.IOApp
import cats.effect.IO
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import org.http4s.HttpRoutes
import cats.effect.MonadCancelThrow
import fs2.compression.Compression
import fs2.io.file.Files
import fs2.compression.DeflateParams
import org.http4s.dsl.Http4sDsl
import cats.implicits._
import cats.effect.implicits._
import cats.effect.kernel.Async
import java.util.zip.ZipOutputStream
import cats.effect.kernel.Sync
import java.util.zip.ZipEntry
import fs2.io.file.Path
import org.http4s.MediaType
import org.http4s.headers._
import java.io.OutputStream

object Main extends IOApp.Simple {

  val run: IO[Unit] =
    BlazeServerBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(Routes.instance[IO].orNotFound)
      .resource
      .productL(IO.println("Started").toResource)
      .useForever

}

object Routes {

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

  def instance[F[_]: Async]: HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of { case GET -> Root / "vscode-extensions" / publisher / name / "latest.zip" =>
      val ext = Extension(publisher, name, "0.0.1", "test")

      Ok(fs2.Stream.eval(src[F].map(replace(_, ext))).through(bytes[F]))
        .map(_.withContentType(`Content-Type`(MediaType.application.zip)))
    }
  }

  def replace(template: String, ext: Extension): String = template
    .replace("TEMPLATE_NAME", ext.name)
    .replace("TEMPLATE_PUBLISHER", ext.publisher)
    .replace("TEMPLATE_VERSION", ext.version)
    .replace("TEMPLATE_SHA256", ext.sha256)

}

final case class Extension(publisher: String, name: String, version: String, sha256: String)
