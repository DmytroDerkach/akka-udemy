package part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {
// part 1 - actor system
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  //part 2 -create actors
  // word count actor

  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0;

    //behavior
    override def receive: Receive = {
      case message: String =>
        println(s"[wordCounter] I've received message: $message")
        totalWords += message.split(" ").length
      case msg => println(s"[wordCounter] I can't understand $msg")
    }
  }

  // part 3 - instantiate the actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")

  //part 4 - communicate
  wordCounter ! "I'm learning akka and it's pretty damn cool!"
  anotherWordCounter ! "a different message"
  //asynchronous

  object Person{
    def props(name: String) = Props(new Person(name))
  }
  class Person(name: String) extends Actor{
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }
  val person = actorSystem.actorOf(Person.props("bob"))
  person ! "hi"
}
