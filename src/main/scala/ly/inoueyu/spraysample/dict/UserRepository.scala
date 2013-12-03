package ly.inoueyu.spraysample.dict

//import scala.util.{Success, Failure}
import spray.json.DefaultJsonProtocol._
//import spray.http._
//import spray.httpx.unmarshalling.{Unmarshaller, pimpHttpEntity}
//import akka.actor.{ActorRef, ActorLogging, Actor}
//import com.scalapenos.riak._

class User(
   id: String
  ,name: String ) {

  def this( id: String ) = this( id, null )
  def this() = this( null, null )

  def getId = id
  def getName = name
}
object User {
//  implicit val userFormUnMarshaller = Unmarshaller[User]( MediaTypes.`multipart/form-data` ) {
//    case HttpEntity.NonEmpty( contentType, buffer ) =>
//      val Array( id, name ) : Array[String] = buffer.asString.split( "&".toCharArray).map( _.trim )
//      User( id, name )
//
//    case HttpEntity.Empty => User("", "")
//
//  }
  def apply( id:String, name:String ) = {
    new User( id, name )
  }
  def apply( id:String ) = {
    new User( id )
  }
  def apply() = {
    new User()
  }
}

//object UserRepositoryProtocol {
//  case class StoreUser( user: User )
//  case class UpdateUser( user: RiakMeta[User] )
//  case class FetchUserById( id: String )
//  case class FetchUsersByName( name: String )
//}
//
///**
// * Created with IntelliJ IDEA.
// * User: a12884
// * Date: 2013/09/18
// * Time: 14:47
// * To change this template use File | Settings | File Templates.
// */
//class UserRepository extends Actor with ActorLogging {
//  import UserRepositoryProtocol._
//  import context.dispatcher
//
//  private val users = RiakClient( context.system, "http://10.200.48.49:8098/riak" ).bucket( "users" )
//
//  def receive = {
//    case StoreUser( user )   => storeUser( user, sender )
//    case UpdateUser( user )  => updateUser( user, sender )
//    case FetchUserById( id ) => fetchUserById( id, sender )
//  }
//
//  private def storeUser( user: User, actor: ActorRef ) {
//    users.storeAndFetch( user.id, user )
//      .map( value => value.asMeta[User] )
//      .onComplete {
//        case Success( storedUserMeta ) =>
//          log.info( "success:" + storedUserMeta.toString() )
//          actor ! storedUserMeta
//        case Failure( e ) => log.error( e, "can't store user data!" )
//      }
//  }
//
//  private def updateUser( userMeta: RiakMeta[User], actor: ActorRef) {
//    users.storeAndFetch( userMeta.data.id, userMeta )
//      .map( value => value.asMeta[User] )
//      .onSuccess {
//        case storedUserMeta => actor ! storedUserMeta
//    }
//  }
//
//  private def fetchUserById( id: String, actor: ActorRef ) {
//    users.fetch( "id", id )
//      .map( valueOption => valueOption.map( _.asMeta[User] ) )
//      .onSuccess {
//        case userMetaOption => actor ! userMetaOption
//      }
//  }
//}
