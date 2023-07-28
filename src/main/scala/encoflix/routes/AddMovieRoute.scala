package encoflix.routes

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import encoflix.domain.MovieDomain.Movie
import encoflix.protocol.MovieProtocol._
import encoflix.util.RouteUtil._

case class AddMovieRoute(movieWriterActor: ActorRef[Command])(implicit scheduler: Scheduler, timeout: Timeout) {
  val route: Route = {
    pathEnd {
      post {
        entity(as[Movie]) { movie =>
          val future = movieWriterActor.ask[AddMovieResponse](AddMovie(movie, _))
          onSuccess(future) {
            case success: AddMovieSuccess => completeCreated(success.movie)
            case failure: AddMovieFailure => completeBadRequest(Map("error" -> failure.reason))
          }
        }
      }
    }
  }
}
