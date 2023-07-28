package encoflix.protocol

import encoflix.domain.MovieDomain.{Movie, MovieId}

object MovieEvent {
  sealed trait MovieEvent
  case class MovieAdded(movie: Movie) extends MovieEvent
  case class MovieUpdated(movie: Movie) extends MovieEvent
  case class MovieDeleted(id: MovieId) extends MovieEvent
}
