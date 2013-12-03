package ly.inoueyu.spraysample.dict

import akka.actor.{ActorRef, ActorLogging, Actor}
import com.basho.riak.client.{IRiakClient, RiakFactory}
import scala.concurrent._
import scala.util.{Failure, Success}
import ExecutionContext.Implicits.global
import com.basho.riak.client.bucket.Bucket

object UserBucketProtocol {
  case class Close()
  case class Create()
  case class StoreUser( user: User )
  case class FetchUserById( id: String )
  case class FetchUsersByName( name: String )
}

/**
* Created with IntelliJ IDEA.
* User: a12884
* Date: 2013/09/19
* Time: 18:05
* To change this template use File | Settings | File Templates.
*/
class UserBucket( peer: ActorRef ) extends Actor with ActorLogging {

  import UserBucketProtocol._

  private val _name = "users"

  private var _client : IRiakClient = null
  private var _bucket : Bucket = null

  def receive = {
    case Close               => close( sender )
    case Create              => create( sender )
    case StoreUser( user )   => storeUser( user, sender )
    case FetchUserById( id ) => fetchUserById( id, sender )
  }

  private def client : IRiakClient = {
    _client = RiakFactory.pbcClient( "10.200.48.49", 8087 )
    _client
  }

  private def bucket : Bucket = {
    if ( _bucket != null )
      _bucket

    log.debug( "bucket is null. so, try to fetch bucket." )

    _bucket = client.fetchBucket( _name ).execute
    _bucket
  }

  private def close( actor: ActorRef ) = {
    _client.shutdown()
    actor ! "status: Riak Connection Closed."
  }

  private def create( actor: ActorRef ) = {
    client.createBucket( _name ).execute
    peer ! "status: New bucket created."
  }

  private def storeUser( user: User, actor: ActorRef ) = {

    store( user ).onComplete {
      case Success( value ) =>
        log.info( "added" + value.getId )
        actor ! value

      case Failure( e ) =>
        log.error( e, "can't add user to riak!" )
    }
  }

  private def fetchUserById( id: String, actor: ActorRef ) = {

    search( id ).onComplete {
      case Success( value ) =>
        log.info( "found:" + value.getId )
        actor ! value

      case Failure( e ) =>
        log.error( e, "not found user id=" + id )
    }
  }

  private def store( user: User ) : Future[User] = {
    future {
      bucket.store( user.getId, user ).execute
    }
  }
  private def search( key: String ) : Future[User] = {
    future {
      bucket.fetch( key, classOf[User] ).execute
    }
  }

}
