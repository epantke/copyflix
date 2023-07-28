package encoflix.service

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.query.EventEnvelope
import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, EventsByPersistenceIdQuery, ReadJournal}
import akka.persistence.typed.PersistenceId
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.typed.scaladsl.ActorSink
import encoflix.domain.MovieDomain.{Movie, MovieId}
import encoflix.protocol.MovieEvent._
import encoflix.protocol.MovieProtocol._
import encoflix.service.MovieReadActor.Protocol._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MovieReadActor {

  object Protocol {
    sealed trait ActorCommand
    sealed trait StreamCommand extends ActorCommand

    case class StateRestored(state: State, sqNr: Long) extends ActorCommand
    case class StateRestoreFailed(cause: Throwable) extends ActorCommand
    case class ProcessQuery(query: Query) extends ActorCommand

    case class InitStream(ackTo: ActorRef[Ack.type]) extends StreamCommand
    case class UpdateState(ackTo: ActorRef[Ack.type], event: MovieEvent) extends StreamCommand
    case object CompleteMsg extends StreamCommand
    case class FailMsg(ex: Throwable) extends StreamCommand
    case object Ack extends StreamCommand
  }

  case class State(movies: Map[MovieId, Movie])
  type Journal = ReadJournal with CurrentEventsByPersistenceIdQuery with EventsByPersistenceIdQuery

  def apply(
      persistenceId: PersistenceId,
      readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery with EventsByPersistenceIdQuery
  ): Behavior[ActorCommand] = {
    Behaviors.setup[ActorCommand] { context =>
      implicit val actorContext: ActorContext[ActorCommand] = context
      implicit val materializer: Materializer = Materializer(context.system)

      val restoreStateVal: Source[EventEnvelope, NotUsed] = readJournal
        .currentEventsByPersistenceId(persistenceId.id, 0L, Long.MaxValue)

      context.pipeToSelf(
        restoreState(persistenceId, restoreStateVal)
      ) {
        case Success((state, seqNr)) => StateRestored(state, seqNr)
        case Failure(e) => StateRestoreFailed(e)
      }

      initializing(persistenceId, readJournal)
    }
  }

  private def initializing(
      persistenceId: PersistenceId,
      readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery with EventsByPersistenceIdQuery
  )(implicit mat: Materializer): Behavior[ActorCommand] =
    Behaviors.withStash(100) { stash =>
      Behaviors.receive { (context, message) =>
        message match {
          case StateRestored(state, seqNr) =>
            stash.unstashAll(initActiveBehaviour(persistenceId, readJournal, context, state, seqNr))
          case StateRestoreFailed(e) =>
            context.log.error("Failed to restore state!", e)
            Behaviors.stopped
          case msg =>
            if (stash.isFull) {
              context.log.warn("Stash buffer is full, dropping message.")
              Behaviors.same
            } else {
              stash.stash(msg)
              Behaviors.same
            }
        }
      }
    }

  private def initActiveBehaviour(
      persistenceId: PersistenceId,
      readJournal: ReadJournal with CurrentEventsByPersistenceIdQuery with EventsByPersistenceIdQuery,
      context: ActorContext[ActorCommand],
      state: State,
      seqNr: Long
  )(implicit materializer: Materializer) = {
    context.log.info("State restored!")

    val liveEvents: Source[EventEnvelope, NotUsed] = readJournal
      .eventsByPersistenceId(persistenceId.id, seqNr + 1, Long.MaxValue)

    val sink: Sink[MovieEvent, NotUsed] =
      ActorSink.actorRefWithBackpressure[MovieEvent, ActorCommand, Ack.type](
        ref = context.self,
        messageAdapter = (ackTo: ActorRef[Ack.type], event: MovieEvent) => Protocol.UpdateState(ackTo, event),
        onInitMessage = (ackTo: ActorRef[Ack.type]) => Protocol.InitStream(ackTo),
        ackMessage = Protocol.Ack,
        onCompleteMessage = Protocol.CompleteMsg,
        onFailureMessage = (exception) => Protocol.FailMsg(exception)
      )

    liveEvents.map(envelope => envelope.event.asInstanceOf[MovieEvent]).runWith(sink)

    activeBehaviour(persistenceId, liveEvents, state, seqNr)(context, Materializer(context.system))
  }

  private def activeBehaviour(
      persistenceId: PersistenceId,
      liveEvents: Source[EventEnvelope, NotUsed],
      state: State,
      seqNr: Long
  )(implicit context: ActorContext[ActorCommand], mat: Materializer): Behavior[ActorCommand] =
    Behaviors.receiveMessage {
      case ProcessQuery(query) =>
        context.log.info(s"Handling query = $query")
        handleQuery(state, query)
        Behaviors.same
      case Protocol.UpdateState(ackTo, event) =>
        context.log.info(s"Received message: $event")
        val newState = updateState(state, event)
        activeBehaviour(persistenceId, liveEvents, newState, seqNr)
        ackTo ! Protocol.Ack
        Behaviors.same
      case Protocol.InitStream(ackTo) =>
        context.log.info("Initialization message received")
        ackTo ! Protocol.Ack
        Behaviors.same
      case Protocol.CompleteMsg =>
        context.log.info("Stream completed")
        Behaviors.same
      case Protocol.FailMsg(ex) =>
        context.log.error("Stream failed with exception", ex)
        Behaviors.same
      case _ =>
        context.log.error("Unexpected input type!")
        Behaviors.same
    }

  private def restoreState(persistenceId: PersistenceId, restoreState: Source[EventEnvelope, NotUsed])(implicit
      context: ActorContext[ActorCommand],
      mat: Materializer
  ): Future[(State, Long)] = {
    context.log.info(s"Restoring state for MovieReadActor with persistenceId = $persistenceId")

    restoreState
      .runFold((State(Map.empty), 0L)) { case ((state, seqNr), envelope) =>
        (updateState(state, envelope.event.asInstanceOf[MovieEvent]), envelope.sequenceNr)
      }
  }

  private def updateState(state: State, event: MovieEvent): State = {
    event match {
      case event: MovieAdded => State(state.movies + (event.movie.id -> event.movie))
      case event: MovieDeleted => State(state.movies - event.id)
      case event: MovieUpdated => State(state.movies + (event.movie.id -> event.movie))
      case _ => state
    }
  }

  private def handleQuery(state: State, query: Query): Unit = {
    query match {
      case qry: GetAllMovies => handleAllMoviesQuery(state, qry)
      case qry: GetMovieById => handleMovieByIdQuery(state, qry)
      case qry: GetAllMoviesByYear => handleMoviesByReleaseYearQuery(state, qry)
      case qry: GetAllYears => handleAllReleaseYearsQuery(state, qry)
    }
  }

  private def handleAllMoviesQuery(state: State, qry: GetAllMovies): Unit = {
    qry.replyTo ! GetMoviesSuccess(state.movies.values.toSeq)
  }

  private def handleMovieByIdQuery(state: State, qry: GetMovieById): Unit = {
    state.movies.get(qry.id) match {
      case Some(movie) => qry.replyTo ! GetMovieSuccess(movie)
      case None => qry.replyTo ! GetMovieFailure(s"No movie found with id ${qry.id}")
    }
  }

  private def handleMoviesByReleaseYearQuery(state: State, qry: GetAllMoviesByYear): Unit = {
    val moviesByYear = state.movies.values.filter(_.year == qry.year).toSeq
    qry.replyTo ! GetMoviesSuccess(moviesByYear)
  }

  private def handleAllReleaseYearsQuery(state: State, qry: GetAllYears): Unit = {
    val years = state.movies.values.map(_.year).toSeq.distinct
    qry.replyTo ! GetYearsSuccess(years)
  }
}
