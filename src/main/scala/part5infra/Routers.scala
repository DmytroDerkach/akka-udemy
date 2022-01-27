package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /**
   #1 manual router
   */
  class Master extends Actor{
    // step 1 create routees
    // 5 actor routees based off Slave actors
    private val slaves = for(i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"name_$i")
      context.watch(slave)

      ActorRefRoutee(slave)
    }
    //step 2 define the router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      // step 4- handle the termination/lifecycle of the routees
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
          //step 3 - route the msgs
      case msg =>
        router.route(msg, sender())
    }
  }

  class Slave extends Actor with ActorLogging{
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }

  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master], "master")
//  for(i <- 1 to 10 ){
//    master ! s"[$i] Hello from the world "
//  }
  /**
   * #2 - a router actor with its own children
   */
  val poolMaster = system.actorOf(RoundRobinPool(4).props(Props[Slave]), "simpleMasterPool")
  for(i <- 1 to 10 ){
//    poolMaster ! s"[$i] Hello from the world "
  }

  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for(i <- 1 to 10 ){
    poolMaster2 ! s"[$i] Hello from the world "
  }
}
