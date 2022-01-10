package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {
  implicit val system = ActorSystem("SynchronousTesting")

  override def afterAll(): Unit ={
    system.terminate()
  }
  import SynchronousTestingSpec._
  "A counter" should{
    "Synchronously increase its counter" in {
      val counter = TestActorRef[CounterActor](Props[CounterActor])
      counter ! Inc // counter has already received a message

      assert(counter.underlyingActor.count == 1)
    }

    "counter receive" in {
      val counter = TestActorRef[CounterActor](Props[CounterActor])
      counter.receive(Inc)

      assert(counter.underlyingActor.count == 1)
    }

    "withDispatcher" in {
      val counter = system.actorOf(Props[CounterActor].withDispatcher(CallingThreadDispatcher.Id))
      val prob = TestProbe()
      prob.send(counter, Read)
      prob.expectMsg(Duration.Zero, 0) // prob has already receive the message 0 (due to line ".withDispatcher(CallingThreadDispatcher.Id))")
    }
  }
}

object SynchronousTestingSpec{
  case object Inc
  case object Read
  class CounterActor extends Actor{
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
