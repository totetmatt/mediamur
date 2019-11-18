package fr.totetmatt.mediamur.actors

import akka.actor.{Actor, ActorLogging, Props}
import fr.totetmatt.mediamur.MediaDTO


import scala.collection.mutable

object MediaManagerActor {

  final case class AskSearchByHashtag(hashtag: String)

  final case class TellAddNewMedia(media: MediaDTO)

  def props: Props = Props[MediaManagerActor]
}

class MediaManagerActor extends Actor with ActorLogging {

  import MediaManagerActor._

  val mediaList = mutable.Set[MediaDTO]()

  override def receive: Receive = {
    case TellAddNewMedia(media) => mediaList.add(media)
    case AskSearchByHashtag(hashtag) => {
      sender() ! mediaList.filter(x => x.status.getHashtagEntities.map(_.getText.toLowerCase).contains(hashtag.toLowerCase)).toSeq
    }
  }
}
