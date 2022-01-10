package part2actors

import akka.actor.{Actor, ActorPath, ActorRef, ActorSystem, Props, actorRef2Scala}

object ChangingActorBehaviorExercise extends App {

  object Counter{
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor{
    import Counter._

    override def receive: Receive = {
      countReceive(0)
    }

    def countReceive(currentCount:Int): Receive = {
      case Increment => context.become(countReceive(currentCount + 1))
      case Decrement => context.become(countReceive(currentCount - 1))
      case Print => println(currentCount)
    }
  }

  import Counter._
  val system = ActorSystem("exercises")
  val counter = system.actorOf(Props[Counter], "counter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 2).foreach(_ => counter ! Decrement)
  counter ! Print

  //-----------------------------------
  println("-----------------------------------")

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor{
    override def receive: Receive = {
      case Vote(c) => context.become(voted(c))//candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(None)

    }
    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor{
    override def receive: Receive = {
        awaitingCommand
    }

    def awaitingCommand :Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(cRef => cRef ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))
    }
    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive = {
      case VoteStatusReply(None) => sender() ! VoteStatusRequest
      case VoteStatusReply(Some(c)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(c, 0)
        val newStats = currentStats + (c -> (currentVotesOfCandidate + 1))
        if(newStillWaiting.isEmpty){
          println(s"[AggregateVotes] currentStats : $newStats")
        }else{
          //still need to process some statuses
          context.become(awaitingStatuses(newStillWaiting, newStats))
        }
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("C1")
  bob ! Vote("C2")
  charlie ! Vote("C3")
  daniel ! Vote("C3")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))


}
