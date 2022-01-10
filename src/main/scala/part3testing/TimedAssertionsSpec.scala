package part3testing

import java.util.Random

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class TimedAssertionsSpec extends TestKit(ActorSystem("TimedAssertionsSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TimedAssertionsSpec._
  "A worker actor" should{
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply" in {
      within(500 millis, 1 second){
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply 2" in {
      within(1 second){
        workerActor ! "workSeq"
        val results = receiveWhile[Int](max = 2 second, idle = 500 millis, messages = 10){
          case WorkResult(n) => n
        }
        assert(results.sum > 5)
      }
    }

    "reply to a test prob" in {
      within(1 second) {
        val prob = TestProbe()
        prob.send(workerActor, "work")
        prob.expectMsg(WorkResult(42)) // timeout of 0.3s
      }
    }
  }
}

object TimedAssertionsSpec {
  // testing scenario

  case class WorkResult(result: Int)
  class WorkerActor extends Actor{
    override def receive: Receive = {
      case "work" =>
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSeq" =>
        val r = new Random()
        for (i <- 1 to 10){
          Thread.sleep(r.nextInt(50))
          sender()! WorkResult(i)
        }
    }
  }
}
