package part1recap

object ThreadModelLimitations extends App {

  class BankAccount(@volatile private var amount: Int){

    override def toString = s"" + amount

    def withdraw(money: Int): Unit =this.synchronized{
      this.amount -= money
    }
    def deposit(money: Int)=this.synchronized{
      this.amount += money
    }

    def getAmount = amount
  }

  val account = new BankAccount(2000)
  for( _ <- 1 to 1000){
    new Thread(() => account.withdraw(1)).start()
  }

  for( _ <- 1 to 1000){
    new Thread(() => account.deposit(1)).start()
  }

  println(account.getAmount)
}
