package encoflix.routes

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import encoflix.domain.MovieDomain.{Movie, mapper}
import encoflix.protocol.MovieProtocol._
import encoflix.service.MovieReadActor.Protocol.{ActorCommand, ProcessQuery}
import encoflix.util.RouteUtil._

case class MovieDetailsRoute(movieWriterActor: ActorRef[Command], movieReaderActor: ActorRef[ActorCommand])(implicit
                                                                                                            scheduler: Scheduler,
                                                                                                            timeout: Timeout
) {
  val route: Route = {
    path(Segment) { id: String =>
      concat(
        get {
          val future = movieReaderActor.ask[GetMovieResponse](replyTo => ProcessQuery(GetMovieById(id, replyTo)))
          onSuccess(future) {
            case success: GetMovieSuccess => completeOk(success.movie)
            case failure: GetMovieFailure => completeNotFound(Map("error" -> failure.reason))
          }
        },
        delete {
          val future = movieWriterActor.ask[DeleteMovieResponse](DeleteMovie(id, _))
          onSuccess(future) {
            case success: DeleteMovieSuccess => completeOk(success.id)
            case failure: DeleteMovieFailure => completeNotFound(Map("error" -> failure.reason))
          }
        },
        put {
          entity(as[Movie]) { updatedMovie =>
            val future = movieWriterActor.ask[UpdateMovieResponse](UpdateMovie(updatedMovie, _))
            onSuccess(future) {
              case success: UpdateMovieSuccess => completeOk(success.movie)
              case failure: UpdateMovieFailure => completeNotFound(Map("error" -> failure.reason))
            }
          }
        }
      )
    }
  }
}
