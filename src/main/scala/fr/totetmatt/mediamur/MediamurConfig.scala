package fr.totetmatt.mediamur

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

case class MediamurConfiguration(track:List[String],records:Boolean)


class MediamurConfig (config: Config) extends Extension {
  val CONSUMER_KEY: String = config.getString("mediamur.twitter.consumer_key")
  val CONSUMER_SECRET: String = config.getString("mediamur.twitter.consumer_secret")
  val ACCESS_TOKEN: String = config.getString("mediamur.twitter.access_token")
  val ACCESS_TOKEN_SECRET: String = config.getString("mediamur.twitter.access_token_secret")

  val TRACK = config.getStringList("mediamur.query.track")

  val WEB_SERVER_HOST: String = config.getString("akka.http.server.host")
  val WEB_SERVER_PORT: Int = config.getInt("akka.http.server.port")

  val RECORD_ACTIVE: Boolean = config.getBoolean("mediamur.recordActive")
  val SAMPLE_STREAM_ACTIVE: Boolean = config.getBoolean("mediamur.sampleStream")

}
object Settings extends ExtensionId[MediamurConfig] with ExtensionIdProvider {

  override def lookup = Settings

  override def createExtension(system: ExtendedActorSystem) =
    new MediamurConfig(system.settings.config)

  override def get(system: ActorSystem): MediamurConfig = super.get(system)
}