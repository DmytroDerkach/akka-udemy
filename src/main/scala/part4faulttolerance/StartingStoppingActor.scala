package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActor extends App {
 val system = ActorSystem("StoppingActorDemo")

  class Parent extends Actor with ActorLogging{
    override def receive: Receive = withChildren(Map())
    import  Parent._
    def withChildren(children: Map[String, ActorRef]):Receive = {
      case StartChild(name) =>
        log.info(s"Starting child with name $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.warning(s"Stopping child with name $name")
        val childRefOpt = children.get(name)
        childRefOpt.foreach(c => context.stop(c))
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>  log.info(message.toString)

    }
  }

  object Parent{
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Child extends Actor with ActorLogging{
    override def receive: Receive = {
      case mess =>
        log.info("mess : " + mess.toString)
    }
  }

  import Parent._

  /**
   * method #1 context.stop
   */
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("/user/parent/child1")
  child ! "Hi kid"
  parent ! StopChild("child1")
  /*for (_ <- 1 to 50){
    child ! "are you there"
  }*/

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hello"
  parent ! Stop
  for(_ <- 1 to 10) { parent ! "parent #####"}
  for(i <- 1 to 100) { child2 ! s"[$i] child2 ----"}


  /**
   * method 2 - using special messages
   */
  val looseChild = system.actorOf(Props[Child])
  looseChild ! "Hello actor"
  looseChild ! PoisonPill
  looseChild ! " actor are you there"

  val child100 = system.actorOf(Props[Child])
  child100 ! "you are about to be terminated"
  child100 ! Kill // throws akka.actor.ActorKilledException: Kill
  child100 ! "you have been terminated"

  /**
   * Death watch
   */

  class Watcher extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"ref has been stoped $ref")

    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watcehdChild")
  val watchedChild = system.actorSelection("/user/watcher/watcehdChild")
  Thread.sleep(500)
  watchedChild ! "hello"
  watchedChild ! PoisonPill
}
