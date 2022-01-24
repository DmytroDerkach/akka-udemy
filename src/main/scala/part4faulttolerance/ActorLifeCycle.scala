package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

object ActorLifeCycle extends App {

  object StartChild
  class LifeCycleActor extends Actor with ActorLogging{

    override def preStart(): Unit = log.info("Starting")

    override def postStop(): Unit = log.info("Stoped")

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeCycleActor], "child")
    }
  }

  val system = ActorSystem("LifeCycleDemo")
  private val parent: ActorRef = system.actorOf(Props[LifeCycleActor], "parent")
  parent ! StartChild
  parent ! PoisonPill
  /*
   [akka://LifeCycleDemo/user/parent] Starting
[akka://LifeCycleDemo/user/parent/child] Starting
[akka://LifeCycleDemo/user/parent/child] Stoped
[akka://LifeCycleDemo/user/parent] Stoped
   */

  /**
   * restart
   */

  object Fail
  object FailChild
  object CheckChild
  object Check
  class Parent extends Actor{
    private val child  = context.actorOf(Props[Child], "supervisedChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("Supervised child started")
    override def postStop(): Unit = log.info("Supervised child stoped")


    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"Supervised actor restated because of ${reason.getMessage}")
    override def postRestart(reason: Throwable): Unit = log.info(s"supervised actor restarted ${reason.getMessage}")

    override def receive: Receive = {
      case Fail => log.info("Child will fail")
        throw new RuntimeException("I fail")
      case Check => log.info("I am ALIVE")
    }
  }

  val supersiver = system.actorOf(Props[Parent], "supersives")
  supersiver ! FailChild
  supersiver ! CheckChild
}
