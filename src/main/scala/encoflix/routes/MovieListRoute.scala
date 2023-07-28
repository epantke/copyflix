package encoflix.routes

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import encoflix.protocol.MovieProtocol._
import encoflix.service.MovieReadActor.Protocol._
import encoflix.util.RouteUtil._

case class MovieListRoute(movieReaderActor: ActorRef[ActorCommand])(implicit scheduler: Scheduler, timeout: Timeout) {
  val route: Route = {
    pathEnd {
      get {
        parameters(Symbol("year").as[Int].?) {
          case Some(yearValue) =>
            val future =
              movieReaderActor.ask[GetMoviesResponse](replyTo => ProcessQuery(GetAllMoviesByYear(yearValue, replyTo)))
            onSuccess(future) { case success: GetMoviesSuccess =>
              completeOk(success.movies)
            }
          case None =>
            val future = movieReaderActor.ask[GetMoviesResponse](replyTo => ProcessQuery(GetAllMovies(replyTo)))
            onSuccess(future) { case success: GetMoviesSuccess =>
              completeOk(success.movies)
            }
        }
      }
    }
  }
}
