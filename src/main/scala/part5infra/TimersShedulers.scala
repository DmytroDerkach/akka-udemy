package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object TimersShedulers extends App {
  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }

  val system = ActorSystem("TimersShedulers")
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")

  system.log.info("Scheduling reminder for simple actor")
  implicit val executionContext = system.dispatcher // or import system.dispatcher
  system.scheduler.scheduleOnce(1 second ){
    actor ! "Reminder"
  }

  val routine = system.scheduler.schedule(1 second, 2 seconds){
    actor ! "bal bla bla"
  }

  system.scheduler.scheduleOnce(6 seconds){
    routine.cancel()
  }

// ---------------------------------
  class SelfClosingActor extends Actor with ActorLogging{
    var shcedule = createTimeOutWindow()
    def createTimeOutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }
    }
    override def receive: Receive = {
      case "timeout" => log.info("stopping myself")
        context.stop(self)
      case msg => log.info(msg.toString + " staying alive")
        shcedule.cancel()
        shcedule = createTimeOutWindow()
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
  system.scheduler.scheduleOnce(250 millis){
    selfClosingActor ! "ping"
  }

  system.scheduler.scheduleOnce(2 seconds){
    system.log.info("sending pong to selfClosingActor")
    selfClosingActor ! "pong"
  }

  /**
   * ---------------------------------
   * Timer
   */

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedSelfCLosingActor extends Actor with ActorLogging with Timers{
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 500 millis) // previous time will be canceled
      case Reminder =>
        log.info("I'm alive")
      case Stop =>
        log.warning("Stoping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerActor = system.actorOf(Props[TimerBasedSelfCLosingActor], "tomerActor")
  system.scheduler.scheduleOnce(5 seconds){
    timerActor ! Stop
  }
}
