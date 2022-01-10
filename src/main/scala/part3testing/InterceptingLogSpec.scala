package part3testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogSpec extends TestKit(ActorSystem("InterceptingLogSpec", ConfigFactory.load().getConfig("intercepting")))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogSpec._
  "Checkout flow" should {
    "correctly log" in {
      val item = "Rock the JVM akka course"
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched", occurrences = 1) intercept{
        // test code
        val checkoutRef = system.actorOf(Props[CheckOutActor])
        checkoutRef ! Checkout(item, creditCard = "1234-1234-1234-1234")
      }
    }
  }
}

object InterceptingLogSpec{
  case class Checkout(item: String, creditCard: String)
  case class AuthorizedCard( creditCard: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  class CheckOutActor extends Actor{

    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])
    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, creditCard) =>
        paymentManager ! AuthorizedCard(creditCard)
        context.become(pendingPayment(item))
    }
    def pendingPayment(item: String): Receive = {
      case PaymentDenied => // TODO
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
    }
    def pendingFulfillment(item: String): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }
  class PaymentManager extends Actor{
    override def receive: Receive = {
      case AuthorizedCard(card) =>
        if(card.startsWith("0")) sender() ! PaymentDenied else
          sender() ! PaymentAccepted
    }
  }
  class FulfillmentManager extends Actor with ActorLogging{
    var orderId = 0
    override def receive: Receive = {
      case DispatchOrder(item) =>
        orderId += 1
        log.info(s"Order $orderId for item $item has been dispatched")
        sender() ! OrderConfirmed
    }
  }
}
