package encoflix.routes

import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.util.Timeout
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.io.JsonEOFException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import encoflix.protocol.MovieProtocol._
import encoflix.service.MovieReadActor.Protocol.ActorCommand

import scala.concurrent.duration._

object MovieRoutes {
  implicit val timeout: Timeout = ConfigFactory.load().getConfig("encoflix").getDuration("timeout").toMillis.millis

  def apply(movieWriterActor: ActorRef[Command], movieReaderActor: ActorRef[ActorCommand])(implicit
                                                                                           scheduler: Scheduler
  ): Route = {
    handleExceptions(exceptionHandler) {
      handleRejections(malformedContentHandler) {
        pathPrefix("movies") {
          concat(
            AddMovieRoute(movieWriterActor).route,
            MovieListRoute(movieReaderActor).route,
            MovieDetailsRoute(movieWriterActor, movieReaderActor).route
          )
        }
      }
    }
  }

  private def malformedContentHandler: RejectionHandler = RejectionHandler
    .newBuilder()
    .handle {
      case MalformedRequestContentRejection(_, ex: UnrecognizedPropertyException) =>
        complete(StatusCodes.BadRequest, s"Invalid JSON format: ${ex.getMessage}")
      case MalformedRequestContentRejection(_, ex: JsonEOFException) =>
        complete(StatusCodes.BadRequest, s"Invalid JSON format: ${ex.getMessage}")
      case MalformedRequestContentRejection(_, ex: JsonParseException) =>
        complete(StatusCodes.BadRequest, s"Invalid JSON format: ${ex.getMessage}")
      case MalformedQueryParamRejection(_, _, Some(ex: NumberFormatException)) =>
        complete(StatusCodes.BadRequest, s"Invalid URL format: ${ex.getMessage}")
      case ex => complete(StatusCodes.InternalServerError, ex)
    }
    .result()

  private def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case ex: NoSuchElementException => complete(StatusCodes.BadRequest, s"NoSuchElementException: ${ex.getMessage}")
    case ex: IllegalArgumentException => complete(StatusCodes.BadRequest, s"IllegalArgumentException: ${ex.getMessage}")
    case ex: UnrecognizedPropertyException =>
      complete(StatusCodes.BadRequest, s"UnrecognizedPropertyException: ${ex.getMessage}")
    case ex => complete(StatusCodes.InternalServerError, ex.getMessage)
  }
}
