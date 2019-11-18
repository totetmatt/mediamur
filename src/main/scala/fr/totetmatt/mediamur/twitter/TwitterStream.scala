package fr.totetmatt.mediamur.twitter

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, OverflowStrategy}
import fr.totetmatt.mediamur.Mediamur.system
import fr.totetmatt.mediamur.actors.RecorderActor
import fr.totetmatt.mediamur.actors.RecorderActor.TellRecordStatus
import fr.totetmatt.mediamur.{MediamurConfig, MediamurConfiguration, Settings}
import twitter4j._
import twitter4j.conf.ConfigurationBuilder

case class TwittAuth(consumerKey:String,consumerSecret:String,accessToken:String,accessTokenSecret:String)

class TwitterStream(implicit materializer:ActorMaterializer) {

  var config: MediamurConfig = Settings(materializer.system)

  var searchQuery : List[String] = List.empty[String]


  var isRunning:Boolean  = false
  val cb = new ConfigurationBuilder
  cb.setOAuthConsumerKey(config.CONSUMER_KEY)
  cb.setOAuthConsumerSecret(config.CONSUMER_SECRET)
  cb.setJSONStoreEnabled(true)
  cb.setDebugEnabled(false)
  cb.setPrettyDebugEnabled(false)
  cb.setOAuthAccessToken(config.ACCESS_TOKEN)
  cb.setOAuthAccessTokenSecret(config.ACCESS_TOKEN_SECRET)

  val recorder  = system.actorOf(RecorderActor.props, "recorder-actor")
  var recordActive = config.RECORD_ACTIVE

  def getRunningConfig = MediamurConfiguration(searchQuery,recordActive)
  def updateRunningConfig(newConfig:MediamurConfiguration) = {
    stopStream()
    recordActive = newConfig.records
    searchQuery = newConfig.track
    startStream(searchQuery,false)

  }
  private val twitterStream = new TwitterStreamFactory(cb.build()).getInstance


  val (tweetQueueMat, tweetQueueSource) = Source
    .queue[Status](100000, OverflowStrategy.dropTail)
    .preMaterialize()

  def getTweetQueueSource: Source[Status, NotUsed] = tweetQueueSource

  val statusListener = new StatusListener() {

    override def onStatus(status: Status): Unit = {
      tweetQueueMat.offer(status)
      if(recordActive) recorder ! TellRecordStatus(status,TwitterObjectFactory.getRawJSON(status))

    }

    override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = {
      // System.err.println(statusDeletionNotice)
    }

    override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = {
      System.err.println(numberOfLimitedStatuses)
    }

    override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = {
      System.err.println(userId)
    }

    override def onStallWarning(warning: StallWarning): Unit = {
      System.err.println(warning)
    }

    override def onException(ex: Exception): Unit = {
      System.err.println(ex)
    }
  }
  twitterStream.addListener(statusListener)

  def startStream(searchQuery:List[String], sampleStream:Boolean): Unit = {
    if(isRunning) {
      stopStream()
    }
    this.searchQuery = searchQuery
   if(searchQuery.isEmpty) {
      if(sampleStream) {
        twitterStream.sample()
      }
    } else {
      twitterStream.filter(searchQuery:_*)
    }
    isRunning = true
  }
  def stopStream(): Unit = {
    twitterStream.shutdown()
    isRunning = false
  }
}