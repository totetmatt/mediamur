package fr.totetmatt.mediamur.actors

import java.io.File
import java.net.URI
import java.nio.file.StandardOpenOption

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import fr.totetmatt.mediamur.actors.RecorderActor.TellRecordStatus
import twitter4j.Status

import scala.concurrent.ExecutionContext.Implicits.global

object RecorderActor {
  implicit val system = ActorSystem("mediamur")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val RECORD_PATH = new java.io.File("./records")
  if (!RECORD_PATH.exists()) RECORD_PATH.mkdir()

  final case class TellRecordStatus(status: Status, content: String)

  def props: Props = Props[RecorderActor]
}

class RecorderActor extends Actor with ActorLogging {

  def recordStatus(status: Status, jsonStatus: String) = {

    val workdir = new java.io.File(RecorderActor.RECORD_PATH, status.getId.toString)

    val statusFile = new java.io.File(RecorderActor.RECORD_PATH, "status.json")
    if (!statusFile.exists()) statusFile.createNewFile()
    Source
      .single(jsonStatus + "\n")
      .map(t => ByteString(t))
      .runWith(FileIO.toPath(statusFile.toPath, Set(StandardOpenOption.APPEND)))(RecorderActor.materializer)

    status.getMediaEntities.foreach { m =>
      if (!workdir.exists()) workdir.mkdir()

      downloadMedia(m.getMediaURLHttps, new java.io.File(workdir, m.getMediaURLHttps.split("/").last))
      if (m.getVideoVariants.nonEmpty) {
        val video = m.getVideoVariants.maxBy(_.getBitrate)
        downloadMedia(video.getUrl, new java.io.File(workdir, new URI(video.getUrl).getPath.split("/").last))
      }
    }

  }

  def downloadMedia(uri: Uri, file: File) = {
    val request = HttpRequest(uri = uri)
    val responseFuture = Http()(this.context.system).singleRequest(request)

    responseFuture.flatMap { response =>

      val source = response.entity.dataBytes
      source.runWith(FileIO.toPath(file.toPath))(RecorderActor.materializer)
    }
  }

  override def receive: Receive = {
    case TellRecordStatus(status, jsonStatus) => recordStatus(status, jsonStatus)
  }
}
