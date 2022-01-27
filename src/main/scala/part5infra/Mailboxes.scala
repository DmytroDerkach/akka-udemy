package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {
  val system = ActorSystem("MailBoxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }

  /**
   * case #1 - custom priority mailbox
   * For instance, if the ticket name start with P0 -> it is the most important
   * then P1, P2, P3
   */

  //step 1 mailbox definition
  class SupportTicketPriorityMailBox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator{
        case message: String if message.startsWith("P0") => 0
        case message: String if message.startsWith("P1") => 1
        case message: String if message.startsWith("P2") => 2
        case message: String if message.startsWith("P3") => 3
        case _ => 4
      })

  // step 2 to make it know in the config
  // step 3 attach to the actor
  val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  supportTicketActor ! PoisonPill // will be postponed
  supportTicketActor ! "P3: message1"
  supportTicketActor ! "P2: message2"
  supportTicketActor ! "P0: message3"
  supportTicketActor ! "P1: message4"
  /*
  output
  [INFO] [01/27/2022 14:36:17.129] [MailBoxDemo-support-ticket-dispatcher-6] [akka://MailBoxDemo/user/$a] P0: message3
  [INFO] [01/27/2022 14:36:17.129] [MailBoxDemo-support-ticket-dispatcher-6] [akka://MailBoxDemo/user/$a] P1: message4
  [INFO] [01/27/2022 14:36:17.129] [MailBoxDemo-support-ticket-dispatcher-6] [akka://MailBoxDemo/user/$a] P2: message2
  [INFO] [01/27/2022 14:36:17.130] [MailBoxDemo-support-ticket-dispatcher-6] [akka://MailBoxDemo/user/$a] P3: message1
     */

  /**
   * case #2 - control-aware mailbox
   * we'll use unbounded control-aware mailbox - UnboundedControlAwareMailBox
   */
  // step 1 - mark important messages as control messages
  case object ManagementTicket extends ControlMessage
  /*
    step 2- config who gets the mail box
    - make the actor attached to the mailbox
   */
  val controlAware = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))
  controlAware ! "P2: P2"
  controlAware ! "P0: P3"
  controlAware ! ManagementTicket

  /**
   * method #2
   */
  val altControlAwareActor = system.actorOf(Props[SimpleActor], "alternativeControlAwareActor")
  altControlAwareActor ! "P2: B2"
  altControlAwareActor ! "P0: B0"
  altControlAwareActor ! ManagementTicket
  /*
  [akka://MailBoxDemo/user/alternativeControlAwareActor] ManagementTicket
  [akka://MailBoxDemo/user/alternativeControlAwareActor] P2: B2
  [akka://MailBoxDemo/user/alternativeControlAwareActor] P0: B0
   */
}
