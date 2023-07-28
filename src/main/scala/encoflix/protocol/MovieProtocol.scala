package encoflix.protocol

import akka.actor.typed.ActorRef
import encoflix.domain.MovieDomain._

object MovieProtocol {

  private type FailureReason = String

  sealed trait Message

  sealed trait Request extends Message

  sealed trait Command extends Request
  case class AddMovie(movie: Movie, replyTo: ActorRef[AddMovieResponse]) extends Command
  case class DeleteMovie(id: MovieId, replyTo: ActorRef[DeleteMovieResponse]) extends Command
  case class UpdateMovie(movie: Movie, replyTo: ActorRef[UpdateMovieResponse]) extends Command

  sealed trait Query extends Request
  case class GetMovieById(id: MovieId, replyTo: ActorRef[GetMovieResponse]) extends Query
  case class GetAllMovies(replyTo: ActorRef[GetMoviesResponse]) extends Query
  case class GetAllMoviesByYear(year: MovieYear, replyTo: ActorRef[GetMoviesResponse]) extends Query
  case class GetAllYears(replyTo: ActorRef[GetYearsResponse]) extends Query

  sealed trait Response extends Message

  sealed trait AddMovieResponse extends Response
  case class AddMovieSuccess(movie: Movie) extends AddMovieResponse
  case class AddMovieFailure(reason: FailureReason) extends AddMovieResponse

  sealed trait DeleteMovieResponse extends Response
  case class DeleteMovieSuccess(id: MovieId) extends DeleteMovieResponse
  case class DeleteMovieFailure(reason: FailureReason) extends DeleteMovieResponse

  sealed trait UpdateMovieResponse extends Response
  case class UpdateMovieSuccess(movie: Movie) extends UpdateMovieResponse
  case class UpdateMovieFailure(reason: FailureReason) extends UpdateMovieResponse

  sealed trait GetMovieResponse extends Response
  case class GetMovieSuccess(movie: Movie) extends GetMovieResponse
  case class GetMovieFailure(reason: FailureReason) extends GetMovieResponse

  sealed trait GetMoviesResponse extends Response
  case class GetMoviesSuccess(movies: Seq[Movie]) extends GetMoviesResponse

  sealed trait GetYearsResponse extends Response
  case class GetYearsSuccess(years: Seq[MovieYear]) extends GetYearsResponse
}
