package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  import SupervisionSpec._

  "A supervisor " should{
    "resume its child in case of minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]

      val child = expectMsgType[ActorRef]
      child ! "I love akka"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome because I am learning to thing in a new way" // will be `RuntimeException` and resumed
      child ! Report
      expectMsg(3)

    }

    "restart its child in case of empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]

      val child = expectMsgType[ActorRef]
      child ! "I love akka"
      child ! Report
      expectMsg(3)

      child ! "" // NullPointerException and will be restarted
      child ! Report
      expectMsg(0)
    }

    "supervisor should terminated a child" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]

      val child = expectMsgType[ActorRef]
      watch(child)
      child ! "akka is nice" // IllegalArgumentException and this child will be stopped

      val teminatedMessage = expectMsgType[Terminated]
      assert(teminatedMessage.actor == child)
    }

    "supervisor should escale error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]

      val child = expectMsgType[ActorRef]
      watch(child)

      child ! 4
      val teminatedMessage = expectMsgType[Terminated]
      assert(teminatedMessage.actor == child)

    }

  }

  "A kinder supervisor" should {
    "not kill children" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 45// will be Escalated
      child ! Report
      expectMsg(0)
    }
  }
}

object SupervisionSpec{
  case object Report

  class Supervisor extends Actor{


    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
      case _ : NullPointerException => Restart
      case _ : IllegalArgumentException => Stop // stop child
      case _ : RuntimeException => Resume
      case _ : Exception => Escalate // Escalate to parant
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor{
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSupersisor extends Supervisor{
    override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy(){
      case _ : NullPointerException => Restart
      case _ : IllegalArgumentException => Stop // stop child
      case _ : RuntimeException => Resume
      case _ : Exception => Escalate // Escalate to parant
    }
  }

  class FussyWordCounter extends Actor{
    var words = 0
    override def receive: Receive = {
      case "" => throw new NullPointerException("is empty")
      case msg: String =>
        if(msg.length > 20 ) throw new RuntimeException("it is too big")
        else if(!Character.isUpperCase(msg(0)))
          throw new IllegalArgumentException("Message should start with uppercase")
        else words += msg.split(" "). length
      case Report => sender() ! words
      case _ => throw new Exception("can only receive strings")
    }
  }
}
