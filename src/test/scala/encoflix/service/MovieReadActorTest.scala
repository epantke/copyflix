package encoflix.service

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, EventsByPersistenceIdQuery, ReadJournal}
import akka.persistence.query.{EventEnvelope, NoOffset}
import akka.persistence.typed.PersistenceId
import akka.stream.scaladsl.Source
import encoflix.domain.MovieDomain._
import encoflix.protocol.MovieProtocol._
import encoflix.service.MovieReadActor.Journal
import encoflix.service.MovieReadActor.Protocol._
import org.scalatest.wordspec.AnyWordSpecLike
import encoflix.protocol.MovieEvent._

class MovieReadActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  private val movie1: Movie = Movie("1", "Test Movie", 2023, 1)
  private val movie2: Movie = Movie("2", "Test Movie 2", 2024, 2)
  private val movie3: Movie = Movie("3", "Test Movie 3", 2025, 3)
  private val movie1Updated: Movie = movie1.copy(name = "Updated Movie")

  "A MovieReadActor" must {

    def setup(): (TestProbe[Response], ActorRef[ActorCommand]) = {
      val probe: TestProbe[Response] = createTestProbe[Response]()
      val movieReadActor: ActorRef[ActorCommand] = spawn(MovieReadActor(PersistenceId.ofUniqueId("MovieReadActorTest"), mockReadJournal))

      Thread.sleep(200)
      (probe, movieReadActor)
    }

    "handle a GetMovieById query" in {
      val (probe, movieReadActor) = setup()

      movieReadActor ! ProcessQuery(GetMovieById(movie1Updated.id, probe.ref))
      probe.expectMessage(GetMovieSuccess(movie1Updated))
    }

    "handle a GetAllMovies query" in {
      val probe = setup()._1
      val movieReadActor = setup()._2

      movieReadActor ! ProcessQuery(GetAllMovies(probe.ref))
      probe.expectMessage(GetMoviesSuccess(Seq(movie1Updated, movie3)))
    }

    "handle a GetAllMoviesByYear query" in {
      val probe = setup()._1
      val movieReadActor = setup()._2

      movieReadActor ! ProcessQuery(GetAllMoviesByYear(2023, probe.ref))
      probe.expectMessage(GetMoviesSuccess(Seq(movie1Updated)))
    }

    "handle a GetAllYears query" in {
      val probe = setup()._1
      val movieReadActor = setup()._2

      movieReadActor ! ProcessQuery(GetAllYears(probe.ref))
      probe.expectMessage(GetYearsSuccess(Seq(2023, 2025)))
    }

    "handle a GetMovieById query for a non-existing movie" in {
      val probe = setup()._1
      val movieReadActor = setup()._2

      movieReadActor ! ProcessQuery(GetMovieById("non-existing-id", probe.ref))
      probe.expectMessage(GetMovieFailure("No movie found with id non-existing-id"))
    }

    "handle a GetAllMoviesByYear query for a non-existing year" in {
      val probe = setup()._1
      val movieReadActor = setup()._2

      movieReadActor ! ProcessQuery(GetAllMoviesByYear(2026, probe.ref))
      probe.expectMessage(GetMoviesSuccess(Seq.empty))
    }
  }

  def mockReadJournal: Journal = new ReadJournal
    with CurrentEventsByPersistenceIdQuery
    with EventsByPersistenceIdQuery {

    val events: Seq[MovieEvent] = Seq(
      MovieAdded(movie1),
      MovieAdded(movie2),
      MovieAdded(movie3),
      MovieUpdated(movie1Updated),
      MovieDeleted(movie2.id)
    )

    val eventEnvelopes: Seq[EventEnvelope] = events.zipWithIndex.map { case (event, index) =>
      EventEnvelope(NoOffset, "MovieActor", index.toLong, event, System.currentTimeMillis())
    }

    override def currentEventsByPersistenceId(
        persistenceId: String,
        fromSequenceNr: Long,
        toSequenceNr: Long
    ): Source[EventEnvelope, NotUsed] =
      Source(eventEnvelopes.filter(e => e.sequenceNr >= fromSequenceNr && e.sequenceNr <= toSequenceNr))

    override def eventsByPersistenceId(
        persistenceId: String,
        fromSequenceNr: Long,
        toSequenceNr: Long
    ): Source[EventEnvelope, NotUsed] =
      Source(eventEnvelopes.filter(e => e.sequenceNr >= fromSequenceNr && e.sequenceNr <= toSequenceNr))
  }
}
