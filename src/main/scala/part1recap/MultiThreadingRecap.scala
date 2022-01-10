package part1recap


import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object MultiThreadingRecap extends App {
  
  // creating threads
  val aThread = new Thread(new Runnable {
    override def run(): Unit = println("run in parallel")
  })

  val aThread2 = new Thread(() => println("thread 2"))
  aThread2.start()
  aThread2.join()

  val threadHello = new Thread(() => (1 to 100).foreach(_ => println("Hello")))
  val threadBye = new Thread(() => (1 to 100).foreach(_ => println("Bye")))

  threadHello.start()
  threadBye.start()

  class BankAccount(@volatile private var amount: Int){

    override def toString = s"" + amount

    def withdraw(money: Int): Unit ={
      this.amount -= money
    }
    def safeWithdraw(money: Int) = this.synchronized{
      this.amount -= money
    }
  }

  //inter-thread communication on the JVM
  // wait-notify
  import scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    // long computation on a diff thread
    42
  }

  //callback
  future.onComplete{
    case Success(42) => println(42)
    case Failure(_) => println("exception")
  }

  val aProcessF = future.map(_ + 1) // Future with 43
  val aFlatF = future.flatMap{v => Future(v+2)} // Future with 44
  val filterF = future.filter(_ % 2 == 0) // NoSuchElementException

  // for comprehensions
  val a = for{
    f <- future
    filtered <- filterF
  } yield f + filtered

  //andThen, recover/recoverWith
}
