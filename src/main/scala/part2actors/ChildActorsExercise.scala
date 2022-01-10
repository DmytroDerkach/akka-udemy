package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {
  // Distributed Word counting

  val system = ActorSystem("system")

  object WordCounterMaster{
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor{
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(n) =>
        println("Master init")
        val childrenRefs = for ( i <- 0 until n) yield context.actorOf(Props[WordCounterWorker], s"child_$i")
        context.become(withChildrenRefs(childrenRefs, 0, 0, Map()))
    }

    def withChildrenRefs(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currenttaskId: Int, requestMap: Map[Int, ActorRef]): Receive ={
      case text: String =>
        println(s"[master] Message is : $text. I will send to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currenttaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newRequestMap = requestMap + (currenttaskId -> originalSender)
        context.become(withChildrenRefs(childrenRefs, nextChildIndex, currenttaskId + 1, newRequestMap))

      case WordCountReply(id, n) =>
        println(s"[master] id of sender $id, counted $n words in message")
        val originalSender  = requestMap(id)
        originalSender ! n
        context.become(withChildrenRefs(childrenRefs, currentChildIndex, currenttaskId, requestMap - id))

    }
  }

  class WordCounterWorker extends Actor{
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, msg) =>
        println(s"${self.path}: I have received text with id $id: $msg")
        val length = msg.split(" ").length
        sender() ! WordCountReply(id, length)
    }
  }

  class TestActor extends Actor{
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val text = List("I love Akka", "Yes", "Me too", "bla bla bla bla")
        text.foreach(t => master ! t)
      case count: Int => println(s"[test actor] reply is $count")
    }
  }

  private val test: ActorRef = system.actorOf(Props[TestActor], "test")
  test ! "go"


  /**
   * create WordCounterMaster
   * send Initialize(10) to WordCounterMaster
   * send ("Akka is awesom") to WordCounterMaster
   *  WordCounterMaster will send WordCountTask("...") to one of the children
   *    child reply with WordCountReply(3) to the master
   *    WordCounterMaster reply with 3 to the sender
   */

  // round robin logic

}
