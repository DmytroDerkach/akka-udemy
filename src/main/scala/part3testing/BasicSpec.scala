package part3testing

import java.util.Random

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import part3testing.BasicSpec.LabTstActor

import scala.concurrent.duration._



class BasicSpec extends TestKit(ActorSystem("BasicSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll{
  override protected def afterAll(): Unit = {

    TestKit.shutdownActorSystem(system)
  }

  "A simple actor " should {
    "send back the same message" in {
      import BasicSpec._
      val echoActor = system.actorOf(Props[SimpleActor])
      val msg = "Hello test"
      echoActor ! msg
      expectMsg(msg) // akka.test.single-expect-default

    }
  }

    "A blackhole" should {
      "send back the some message" in{
        import BasicSpec._
        val blackhole = system.actorOf(Props[Blackhole])
        val msg = "Hello test"
        blackhole ! msg
        expectNoMessage(3 second)
      }
    }


  // message assertions
  "LabTstActor" should {
    val labtextActor = system.actorOf(Props[LabTstActor])
    "turn str" in {
      import BasicSpec._
      labtextActor ! "I love akka"
      val reply = expectMsgType[String]
      assert(reply ==  "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labtextActor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }

    "reply with with 2" in {
      labtextActor ! "favoriteTech"
      expectMsgAnyOf("Scala", "Akka")
    }

    "reply with cool tech in different way" in {
      labtextActor ! "favoriteTech"
      val msgs = receiveN(2) // Seq[Any]
    }

    "reply with cool tech in a fancy way" in {
      labtextActor ! "favoriteTech"
      expectMsgPF(){
        case "Scala" => // we only care that partial function defined
        case "Akka" =>
      }
    }
  }
}

object BasicSpec{
 class SimpleActor extends Actor{
   override def receive: Receive = {
     case msg => sender() ! msg
   }
 }

  class Blackhole extends Actor{
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTstActor extends Actor{
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        if(random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case msg: String => sender() ! msg.toUpperCase()
    }
  }
}
