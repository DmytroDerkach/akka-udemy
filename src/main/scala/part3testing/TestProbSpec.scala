package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbSpec extends TestKit(ActorSystem("TestProbSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import  TestProbSpec._

  "MasterActor" should{
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationMsg)
    }

    "send send a work to a slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationMsg)

      val text = "I love akka"
      master ! Work(text)

      // the interaction between master actor & slave actor
      slave.expectMsg(SlaveWork(text, testActor))
      slave.reply(WorkCompleted(3, testActor)) // mocking data

      expectMsg(Report(3)) // testActor obtains Report(3) from master actor which have reply from slave actor
    }

    "master actor should aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationMsg)

      val text = "I love akka"
      master ! Work(text)
      master ! Work(text)

      slave.receiveWhile(){
        case SlaveWork(`text`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }
      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }

}

object TestProbSpec{
  // scenario

  /**
   * word counting actor hierarchy master-slave
   * - send some word to the master
   * - master sends the slave a piece of work
   * - slave processes the work and replies to the master
   * - master aggregate the results
   * - master sends the total count to the original requester
   */

  case class Register(slaveRef: ActorRef)
  case class Work(text: String)
  case class SlaveWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(totalWordCount: Int)
  case object RegistrationMsg
  class Master extends Actor {
    override def receive: Receive = {

      case Register(slaveRef: ActorRef) =>
      sender() ! RegistrationMsg
        context.become(online(slaveRef, 0))
      case _ =>
    }

    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
    }
  }

  //class Slave extends Actor ...
}
