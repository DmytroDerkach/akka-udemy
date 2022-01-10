package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}
import part2actors.ChildActors.NaiveBankAccount.{Deposit, InitAccount}
import part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {
  // Actors can create other actors (by using `context`)

  val system = ActorSystem("system")

  object Parent{
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor{
    import Parent._
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"Parent is going to create child with name : $name")
        // create new actor
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }

    def withChild(child: ActorRef): Receive = {
      case TellChild(msg) => child forward msg
    }
  }

  class Child extends Actor{
    override def receive: Receive = {
      case message: String => println(s"${self.path} : I've got $message")

    }
  }

  val parent = system.actorOf(Props[Parent], "parent")
  parent ! CreateChild("Dima")
  parent ! TellChild("hey kid")

  // it gives us ability to create actor hierarchies

  /*Actor selection*/
  val childSelection = system.actorSelection("/user/parent/Dima")
  childSelection ! "I've found you"

println("---------------------")
  /**
   * NEVER PASS MUTABLE ACTOR STATE, OR `THIS` REF TO CHILD ACTORS
   */

  object NaiveBankAccount{
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitAccount
  }
  class NaiveBankAccount extends Actor{
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0
    override def receive: Receive = {
      case InitAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this)
      case Deposit(n) => deposit(n)
      case Withdraw(n) => withdraw(n)
    }

    def deposit(funds: Int) = {
      println(s"${self.path} deposit : $funds")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdraw : $funds from amount $amount")
      amount -= funds
    }
  }

  object CreditCard{
    case class AttachToAccount(bankAccount: NaiveBankAccount) // !!!
    case object CheckStatus
  }
  class CreditCard extends Actor{
    import  CreditCard._
    override def receive: Receive = {
      case AttachToAccount(account) =>
        context.become(attachedToAccount(account))
    }
    def attachedToAccount(account: NaiveBankAccount): Receive = {
      case CheckStatus => println(s"${self.path} [attachedToAccount]")
        account.withdraw(1)
    }
  }

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitAccount
  bankAccountRef ! Deposit(150)

  Thread.sleep(500)
  val ccSelecttion = system.actorSelection("/user/account/card")
  ccSelecttion ! CheckStatus
}
