package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App {

  object FussyKid{
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class FussyKid extends Actor{
    //internal state of kid
    import FussyKid._
    import Mom._
    var state: String = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if(state == HAPPY) sender()!KidAccept
        else sender() ! KidReject
    }
  }

  class StatelessFussyKid extends Actor{
    import FussyKid._
    import Mom._

    override def receive: Receive = {
      happyReceive
    }

    def happyReceive: Receive = {
      case Food(VEGETABLE) =>
        //change receive handler to sadReceive
        context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => //change receive handler to happyReceive
        context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
  }

  object Mom {
    case class MomStart(kid: ActorRef)
    case class Food(food: String)
    case class Ask(msg: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("msg")
      case KidAccept => println("[Mom] KidAccept")
      case KidReject => println("[Mom] KidReject")
    }
  }

  val system = ActorSystem("changingActorBehavior")
  val fussyKid = system.actorOf(Props[FussyKid], "fussyKid" )
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessFussyKid" )
  val mom = system.actorOf(Props[Mom], "mom" )

//  mom ! MomStart(fussyKid)
  mom ! MomStart(statelessFussyKid)
}
