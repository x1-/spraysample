package ly.inoueyu.spraysample

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http

/**
 * Created with IntelliJ IDEA.
 * User: a12884
 * Date: 2013/09/17
 * Time: 22:38
 * To change this template use File | Settings | File Templates.
 */
object Main extends App {

  implicit val system = ActorSystem()
  val handler = system.actorOf(Props[DemoService], name = "handler")
  IO(Http) ! Http.Bind(handler, interface = "localhost", port = 8080)
}
