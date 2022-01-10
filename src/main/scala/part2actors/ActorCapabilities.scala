package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import jdk.nashorn.internal.runtime.OptimisticReturnFilters

object ActorCapabilities extends App{

  class SimpleActor extends Actor{
    override def receive: Receive = {
      case "HI" => context.sender() ! "Hello there" // replying to the message
      case message:String => println(s"[${self}] I've received: $message")
      case number: Int => println(s"[$self] I've received number $number")
      case SpecialMessage(content) => println(s"special message: ${content}")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "HI"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s100500")
    }
  }

  val system = ActorSystem("actor")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  simpleActor ! "Hello Actor"

  // 1 - messages can be of any type
  simpleActor ! 42

  case class SpecialMessage(content: String)
  simpleActor ! SpecialMessage("content")

  // 2- actors have information about their context & about themselves
  //context.self === this in OOP
  //context.self === self

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I'm an actor")

  // 3- actors can reply to msg

  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "HI"

  //5 - forwarding messages
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("HI", bob)


}
