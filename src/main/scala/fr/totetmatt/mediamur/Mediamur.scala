package fr.totetmatt.mediamur

import java.io.StringWriter

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.HttpApp
import akka.pattern.ask
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import fr.totetmatt.mediamur.actors.MediaManagerActor
import fr.totetmatt.mediamur.twitter.TwitterStream
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._
import twitter4j.{MediaEntity, Status}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

case class MediaDTO(media: MediaEntity, status: Status)

import scala.language.postfixOps


/*trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val statusFormat= jsonFormat[Status]()


}*/
trait JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {

}

object Mediamur extends HttpApp {

  implicit val system: ActorSystem = ActorSystem("mediamur")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  type UserCount = (String, Int)
  val settings = Settings(system)
  val topUser = scala.collection.mutable.Map[String, Int]()

  val stream = new TwitterStream()

  val mediaManager = system.actorOf(MediaManagerActor.props, "media-manager")


  implicit val mediamurConfigFormat = jsonFormat2(MediamurConfiguration)
  stream.tweetQueueSource.runWith(Sink.ignore)
  stream.startStream(settings.TRACK.asScala.toList,settings.SAMPLE_STREAM_ACTIVE)

  val (verifiedQueueMat, verifiedQueueSource) = Source
    .queue[String](100000, OverflowStrategy.dropTail).preMaterialize()

  override val routes = {
    import DefaultJsonProtocol._
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    path("") {
      get {
        getFromResource("ui/intro.html")
      }
    } ~ path("query") {
      get {
        complete {
          stream.searchQuery
        }
      } ~
        post {
          entity(as[Seq[String]]) { track =>
            complete {
              stream.startStream(track.toList, false)
              stream.searchQuery
            }
          }
        }
    } ~ path("configuration") {
      get {
        complete {
          stream.getRunningConfig
        }
      } ~
        post {
          entity(as[MediamurConfiguration]) { newConfig =>
            complete {

              stream.updateRunningConfig(newConfig)
              newConfig
            }
          }
        }
    } ~ path("verified") {
      post {
        entity(as[String]) { data =>
          verifiedQueueMat.offer(data)
          complete("Ok")
        }
      }
    } ~ pathPrefix("stream") {
      path("verified") {
        get {
          complete {
            verifiedQueueSource
              .map(x => ServerSentEvent(x, "video"))
              .keepAlive(1.second, () => ServerSentEvent.heartbeat)
          }
        }
      } ~
        path("mediahashtag" / Segment) { hashtag =>
          get {
            complete {

              implicit val timeout: Timeout = 2.seconds
              (mediaManager ? MediaManagerActor.AskSearchByHashtag(hashtag))
                .mapTo[Seq[MediaDTO]]
                .map(x=> toJson(x))
            }
          }
        } ~
        path("media") {
          get {
            complete {
              stream
                .tweetQueueSource
                .map(x => if (x.getRetweetedStatus == null) x else x.getRetweetedStatus)
                .mapConcat(x => x.getMediaEntities.toList.map { y =>
                  val media = MediaDTO(y, x)
                  mediaManager ! MediaManagerActor.TellAddNewMedia(media)
                  ServerSentEvent(toJson(media), y.getType)
                })
                .keepAlive(1.second, () => ServerSentEvent.heartbeat)
            }
          }
        } ~
        path("tweets") {
          get {
            complete {
              stream
                .tweetQueueSource
                .map(x => ServerSentEvent(toJson(x), "tweet"))
                .keepAlive(1.second, () => ServerSentEvent.heartbeat)
            }
          }
        }
    } ~ pathPrefix("ui") {
      getFromResourceDirectory("ui")
    } ~ pathPrefix("static") {
      getFromResourceDirectory("static")
    }
  }

  def toJson(m: MediaDTO) = {
    val out = new StringWriter
    mapper.writeValue(out, m)
    out.toString
  }

  def toJson(s: Status) = {
    val out = new StringWriter
    mapper.writeValue(out, s)
    out.toString
  }

  def toJson(l:Seq[MediaDTO]) = {
    val out = new StringWriter
    mapper.writeValue(out, l)
    out.toString
  }

  def main(args: Array[String]): Unit = {
    Mediamur.startServer(settings.WEB_SERVER_HOST, settings.WEB_SERVER_PORT)
    system.terminate()
  }
}
