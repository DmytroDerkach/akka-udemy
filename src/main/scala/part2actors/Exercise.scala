package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.Exercise.BankAccount.{Deposit, Statement, Withdraw}
import part2actors.Exercise.Counter.{Decrement, Increment, Print}
import part2actors.Exercise.Person.LiveTheLife

object Exercise extends App {

  val actorSystem = ActorSystem("system")

  object Counter{
    case class Increment(amount: Int)
    case class Decrement(amount: Int)
    case class Print()
  }

  class Counter extends Actor {
    var total = 0;
    override def receive: Receive = {
      case Increment(amount) =>
        total += amount
        println(s"[Increment] total : $total")
      case Decrement(amount) =>
        total -= amount
        println(s"[Decrement] total : $total")
      case Print => println(s"total : $total")
    }
  }



  val counter = actorSystem.actorOf(Props[Counter], "counter")
  val counter2 = actorSystem.actorOf(Props[Counter], "counter2")
  for(_ <- 1 to 10) counter ! Increment(2)
  for(_ <- 1 to 5) counter2 ! Decrement(1)

  counter ! Print
  counter2 ! Print

  object BankAccount {

    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case class Statement()

    case class Response(msg: String)

  }
  class BankAccount extends Actor{
    var total = 0;
    import BankAccount._
    override def receive: Receive = {
      case Deposit(amount) =>
        total += amount
        println(s"[Deposit] Success, total is : $total")
        sender() ! Response(s"[Deposit] Success, total is : $total")
      case Withdraw (amount) =>
        total -= amount
        println(s"[Withdraw] Success, total is : $total")
        sender() ! Response(s"[Withdraw] Success, total is : $total")
      case Statement =>
        println(s"[Statement] total is : $total")
        sender() ! Response(s"[Statement] total is : $total")
      case Response(msg) => println(s"[Response] : $msg")
    }
  }

  object Person{
    case class LiveTheLife(ref: ActorRef)
  }
  class Person extends Actor {
    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(1000)
        account ! Withdraw(450)
        account ! Statement
      case message => println(s"message from Person: ${message.toString}")
    }
  }
  import BankAccount._
  val bankAccountActor = actorSystem.actorOf(Props[BankAccount], "bank")
  val person = actorSystem.actorOf(Props[Person], "person")

  person ! LiveTheLife(bankAccountActor)
}
