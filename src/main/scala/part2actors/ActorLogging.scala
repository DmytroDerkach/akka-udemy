package part2actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}

object ActorLogging extends App {
 // #1  Explicit logging
  class SimpleActorWithExplicitLogger extends Actor{
    val logger: LoggingAdapter = Logging(context.system, this)

    override def receive: Receive = {
      case message => logger.info(message.toString)
    }
  }

  private val systemSystem: ActorSystem = ActorSystem("system")
  private val actorRef: ActorRef = systemSystem.actorOf(Props[SimpleActorWithExplicitLogger])
  actorRef ! "simple message"


  // #2 Actor loggin
  class ActorWithLogging extends Actor with ActorLogging{
    override def receive: Receive = {
      case (a, b) => log.info(s"two params: {} & {}", a, b)
      case msg => log.info(msg.toString )
    }
  }

  private val ac: ActorRef = systemSystem.actorOf(Props[ActorWithLogging])
  ac ! "new message"
  ac ! (2, 3)
}
