package part5infra

import java.util.Random

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContextExecutor, Future}

object Dispatchers extends App {
  class Counter extends Actor with ActorLogging{
    var counter = 0;
    override def receive: Receive = {
      case msg =>
        counter += 1
        log.info(s"${msg.toString} with count [$counter]")
    }

  }

  val system = ActorSystem("DispatcherDemo") // , ConfigFactory.load().getConfig("dispatcherDemo")

  //#1 - programatic
  val actors = for(i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  val r = new Random()
  for(i <- 1 to 1000){
//    actors(r.nextInt(10)) ! i
  }

  // #2 - from config
  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")


  /**
   * Dispatchers implement the ExecutionContext trait
   */
  class DBActor extends Actor with ActorLogging{
    // solution 1
    implicit val executionContext: ExecutionContextExecutor = context.system.dispatchers.lookup("my-dispatcher")
    // solution #2 - use router
    override def receive: Receive = {
      case msg => Future {
        // wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: $msg")
      }
    }
  }

  private val dbActor: ActorRef = system.actorOf(Props[DBActor])
//  dbActor ! 42

  val nonBLockingActor = system.actorOf(Props[Counter])
  for(i <- 1 to 1000){
    val msg = s"important msg $i"
    dbActor ! msg
    nonBLockingActor ! msg
  }
}
