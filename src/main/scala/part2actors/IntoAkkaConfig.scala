package part2actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntoAkkaConfig extends App {


  class LoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case m => log.info(m.toString)
    }
  }


  /**
   * 1- inline config
   */

  val configString =
    """
      |akka{
      | loglevel = "ERROR"
      |}
      |""".stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("confingDemo", ConfigFactory.load(config))

  private val ref: ActorRef = system.actorOf(Props[LoggingActor])
  ref ! "message"

  /**
   * 2 - config in a file
   */

  val defaultConfig = ActorSystem("DefaultConfig")
  private val defaultConfigActor: ActorRef = defaultConfig.actorOf(Props[LoggingActor])
  defaultConfigActor ! "remember me"

  /**
   * 3
   */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("specialConfigSystem", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[LoggingActor])
  specialConfigActor ! "remember ! me"

  /**
   * 4 -separate config in another file
   *
   */

  val separateConfig = ConfigFactory.load("secretFolder/secretConfig.conf")
  println(s"separateConfig: ${separateConfig.getString("akka.loglevel")}")

  /**
   * 5 - different file formats
   * json, properties
   */
  val separateJSONConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"separateJSONConfig: ${separateJSONConfig.getString("akka.loglevel")}")

  val separatePropsConfig = ConfigFactory.load("props/propsConfig.properties")
  println(s"separatePropsConfig: ${separatePropsConfig.getString("akka.loglevel")}")

}
