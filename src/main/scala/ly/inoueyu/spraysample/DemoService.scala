package ly.inoueyu.spraysample

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import akka.actor._
import spray.can.Http
import spray.can.server.Stats
import spray.util._
import spray.http._
import scala.util.matching.Regex

//import spray.httpx.unmarshalling.Deserializer._
import spray.httpx.unmarshalling.Deserializer
import HttpMethods._
import MediaTypes._

import ly.inoueyu.spraysample.dict._

/**
 * Created with IntelliJ IDEA.
 * User: a12884
 * Date: 2013/09/17
 * Time: 22:40
 * To change this template use File | Settings | File Templates.
 */
class DemoService extends Actor with ActorLogging {
  implicit val timeout: Timeout = 1.second // for the actor 'asks'
  import context.dispatcher // ExecutionContext for the futures and scheduler

  // a map from connections to chunk handlers
  // will be created on ChunkedRequestStart and removed on ChunkedRequestEnd
  var chunkHandlers = Map.empty[ActorRef, ActorRef]

  val repo = context.actorOf( Props(new UserBucket(sender)), "repo" )

  def receive = {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
      sender ! index

    case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
      sender ! HttpResponse(entity = "PONG!")

    case HttpRequest(GET, Uri.Path("/stream"), _, _, _) =>
      val peer = sender // since the Props creator is executed asyncly we need to save the sender ref
      context actorOf Props(new Streamer(peer, 25))

    case HttpRequest(GET, Uri.Path("/server-stats"), _, _, _) =>
      val client = sender
      context.actorFor("/user/IO-HTTP/listener-0") ? Http.GetStats onSuccess {
        case x: Stats => client ! statsPresentation(x)
      }

    case HttpRequest(GET, Uri.Path("/crash"), _, _, _) =>
      sender ! HttpResponse(entity = "About to throw an exception in the request handling actor, " +
        "which triggers an actor restart")
      sys.error("BOOM!")

    case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
      sender ! HttpResponse(entity = "Shutting down in 1 second ...")
      context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/timeout" =>
      log.info("Dropping request, triggering a timeout")

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/user" =>
      log.info("Get User!")
      val rex : Regex = """\/user\/([0-9]+)""".r
      path match {
        case rex( q ) =>
          val peer = sender
          val b = context.actorOf( Props( new UserBucket(peer) ) )
          b ! UserBucketProtocol.FetchUserById( q )
          //repo.tell( UserBucketProtocol.FetchUserById( q ), sender )
          //sender ! HttpResponse(entity = "Starting to get User:" + q )
        case _ =>
          sender ! HttpResponse(entity = "Not Match User Id.")
      }

    case HttpRequest(GET, Uri.Path("/create"), _, _, _) =>
      log.info("Starting Create New Riak Bucket....")
      repo ! UserBucketProtocol.Create

    case HttpRequest(GET, Uri.Path("/close-riak"), _, _, _) =>
      log.info("Shutting Down Riak Connection....")
      repo ! UserBucketProtocol.Close
      sender ! HttpResponse(entity = "ADDED!")

    case HttpRequest(POST, Uri.Path("/user"), headers, entity, _) =>
      log.info("Now, trying to add user to riak...")
      log.info("entity:" + entity.asString(HttpCharsets.`UTF-8`))

      val fd = Deserializer.FormDataUnmarshaller(entity)
      log.info("des:id:" + fd.get.fields("id"))
      log.info("des:name:" + fd.get.fields("name"))

      //repo ! UserRepositoryProtocol.StoreUser( User( fd.get.fields("id"), fd.get.fields("name") ) )
      repo ! UserBucketProtocol.StoreUser( new User( fd.get.fields("id"), fd.get.fields("name") ) )

      sender ! HttpResponse(entity = "ADDED!")

    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")

    case c: MessageChunk => chunkHandlers(sender).tell(c, sender)
    case e: ChunkedMessageEnd =>
      chunkHandlers(sender).tell(e, sender)
      chunkHandlers -= sender

    case Timedout(HttpRequest(_, Uri.Path("/timeout/timeout"), _, _, _)) =>
      log.info("Dropping Timeout message")

    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      sender ! HttpResponse(
        status = 500,
        entity = "The " + method + " request to '" + uri + "' has timed out..."
      )

    case s: String if s startsWith "status:" =>
      sender ! HttpResponse(entity = s)

    case u: User =>
      log.debug( "FOUNDED USER and Presentation Now!" )
      sender ! userPresentation( u )
  }

  ////////////// helpers //////////////

  lazy val index = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>Say hello to <i>spray-can</i>!</h1>
          <p>Defined resources:</p>
          <ul>
            <li><a href="/ping">/ping</a></li>
            <li><a href="/stream">/stream</a></li>
            <li><a href="/server-stats">/server-stats</a></li>
            <li><a href="/crash">/crash</a></li>
            <li><a href="/timeout">/timeout</a></li>
            <li><a href="/timeout/timeout">/timeout/timeout</a></li>
            <li><a href="/stop">/stop</a></li>
          </ul>
          <p>Riak Operations:</p>
          <ul>
            <li><a href="/create">/create</a></li>
            <li><a href="/close-riak">/close-riak</a></li>
            <li><a href="/user">/user/xxx</a></li>
          </ul>
          <p>user data post</p>
          <form action ="/user" enctype="application/x-www-form-urlencoded" method="post">
            <input type="text" name="id" value=""></input>
            <br/>
            <input type="text" name="name" value=""></input>
            <br/>
            <input type="submit">Submit</input>
          </form>
        </body>
      </html>.toString()
    )
  )
  def statsPresentation(s: Stats) = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h1>HttpServer Stats</h1>
          <table>
            <tr><td>uptime:</td><td>{s.uptime.formatHMS}</td></tr>
            <tr><td>totalRequests:</td><td>{s.totalRequests}</td></tr>
            <tr><td>openRequests:</td><td>{s.openRequests}</td></tr>
            <tr><td>maxOpenRequests:</td><td>{s.maxOpenRequests}</td></tr>
            <tr><td>totalConnections:</td><td>{s.totalConnections}</td></tr>
            <tr><td>openConnections:</td><td>{s.openConnections}</td></tr>
            <tr><td>maxOpenConnections:</td><td>{s.maxOpenConnections}</td></tr>
            <tr><td>requestTimeouts:</td><td>{s.requestTimeouts}</td></tr>
          </table>
        </body>
      </html>.toString()
    )
  )
  def userPresentation(u: User) = HttpResponse(
    entity = HttpEntity(`text/html`,
      <html>
        <body>
          <h2>User Found!</h2>
          <table>
            <tr><td>id:</td><td>{u.getId}</td></tr>
            <tr><td>name:</td><td>{u.getName}</td></tr>
          </table>
        </body>
      </html>.toString()
    )
  )
  class Streamer(client: ActorRef, count: Int) extends Actor with ActorLogging {
    log.debug("Starting streaming response ...")

    // we use the successful sending of a chunk as trigger for scheduling the next chunk
    client ! ChunkedResponseStart(HttpResponse(entity = " " * 2048)).withAck(Ok(count))

    def receive = {
      case Ok(0) =>
        log.info("Finalizing response stream ...")
        client ! MessageChunk("\nStopped...")
        client ! ChunkedMessageEnd
        context.stop(self)

      case Ok(remaining) =>
        log.info("Sending response chunk ...")
        context.system.scheduler.scheduleOnce(100 millis span) {
          client ! MessageChunk(DateTime.now.toIsoDateTimeString + ", ").withAck(Ok(remaining - 1))
        }

      case x: Http.ConnectionClosed =>
        log.info("Canceling response stream due to {} ...", x)
        context.stop(self)
    }

    // simple case class whose instances we use as send confirmation message for streaming chunks
    case class Ok(remaining: Int)
  }

}
