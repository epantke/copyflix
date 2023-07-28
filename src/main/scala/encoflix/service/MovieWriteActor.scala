package encoflix.service

import akka.actor.typed.{BackoffSupervisorStrategy, Behavior}
import akka.event.slf4j.Logger
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl._
import encoflix.protocol.MovieEvent._
import encoflix.protocol.MovieProtocol._

object MovieWriteActor {

  private val log = Logger("MovieWriteActorLogger")

  def apply(persistenceId: PersistenceId, supervisorStrategy: BackoffSupervisorStrategy): Behavior[Command] =
    EventSourcedBehavior[Command, MovieEvent, Unit](
      persistenceId,
      emptyState = (),
      commandHandler = (_, command) => onCommand(command),
      eventHandler = (_, _) => ()
    ).withRetention(
      RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2)
    ).onPersistFailure(supervisorStrategy)

  private def onCommand(command: Command): ReplyEffect[MovieEvent, Unit] = {
    log.info(s"MovieWriteActor.onCommand: command = $command")
    command match {
      case cmd: AddMovie => handleAdd(cmd)
      case cmd: DeleteMovie => handleDelete(cmd)
      case cmd: UpdateMovie => handleUpdate(cmd)
    }
  }

  private def handleAdd(addMovie: AddMovie): ReplyEffect[MovieEvent, Unit] =
    if (!isValidRating(addMovie.movie.rating)) {
      Effect.reply(addMovie.replyTo)(AddMovieFailure(s"Rating [${addMovie.movie.rating}] is not valid."))
    } else {
      Effect.persist(MovieAdded(addMovie.movie)).thenReply(addMovie.replyTo)(_ => AddMovieSuccess(addMovie.movie))
    }

  private def handleDelete(deleteMovie: DeleteMovie): ReplyEffect[MovieEvent, Unit] =
    Effect
      .persist(MovieDeleted(deleteMovie.id))
      .thenReply(deleteMovie.replyTo)(_ => DeleteMovieSuccess(deleteMovie.id))

  private def handleUpdate(updateMovie: UpdateMovie): ReplyEffect[MovieEvent, Unit] =
    if (!isValidRating(updateMovie.movie.rating)) {
      Effect.reply(updateMovie.replyTo)(
        UpdateMovieFailure(
          s"Rating [${updateMovie.movie.rating}] for movie [" + updateMovie.movie.id + "] is not valid."
        )
      )
    } else {
      Effect
        .persist(MovieUpdated(updateMovie.movie))
        .thenReply(updateMovie.replyTo)(_ => UpdateMovieSuccess(updateMovie.movie))
    }

  private def isValidRating(rating: Int): Boolean = rating >= 0 && rating <= 3
}
