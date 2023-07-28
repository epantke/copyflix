package encoflix.service

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.{ActorRef, SupervisorStrategy}
import akka.persistence.testkit.scaladsl.{EventSourcedBehaviorTestKit, PersistenceTestKit}
import akka.persistence.typed.PersistenceId
import encoflix.domain.MovieDomain._
import encoflix.protocol.MovieProtocol._
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpecLike
import encoflix.protocol.MovieEvent._

class MovieWriteActorTest extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config) with AnyWordSpecLike {

  "A MovieWriteActor" must {

    val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

    def setup(): (TestProbe[Response], ActorRef[Command], Movie, Movie, Movie) = {
      persistenceTestKit.clearAll()

      val probe = createTestProbe[Response]()
      val movieWriteService = spawn(
        MovieWriteActor(
          PersistenceId.ofUniqueId("MovieActor"),
          SupervisorStrategy.restartWithBackoff(1.second, 5.seconds, 0.1)
        )
      )

      val movie1 = Movie("1", "Test Movie", 2023, 1)
      val movie2 = Movie("2", "Test Movie 2", 2024, 2)
      val movie3 = Movie("3", "Test Movie 3", 2025, 3)

      (probe, movieWriteService, movie1, movie2, movie3)
    }

    "add a movie successfully" in {
      val (probe, movieWriteService, movie1, _, _) = setup()

      movieWriteService ! AddMovie(movie1, probe.ref)
      probe.expectMessage(AddMovieSuccess(movie1))
      persistenceTestKit.persistedInStorage("MovieActor")
      persistenceTestKit.expectNextPersistedType[MovieAdded]("MovieActor").movie shouldBe movie1
    }

    "add a movie with an existing ID" in {
      val (probe, movieWriteService, movie1, movie2, _) = setup()

      movieWriteService ! AddMovie(movie1, probe.ref)
      probe.expectMessage(AddMovieSuccess(movie1))
      persistenceTestKit.expectNextPersistedType[MovieAdded]("MovieActor").movie shouldBe movie1

      movieWriteService ! AddMovie(movie2.copy(id = movie1.id), probe.ref)
      probe.expectMessage(AddMovieSuccess(Movie("1", "Test Movie 2", 2024, 2)))
      persistenceTestKit.expectNothingPersisted("MovieWriteActor")
    }

    "delete a movie if it doesn't exist" in {
      val (probe, movieWriteService, movie1, _, _) = setup()

      movieWriteService ! DeleteMovie(movie1.id, probe.ref)
      probe.expectMessage(DeleteMovieSuccess("1"))
      persistenceTestKit.expectNextPersistedType[MovieDeleted]("MovieActor").id shouldBe movie1.id
    }

    "update a movie" in {
      val (probe, movieWriteService, movie1, _, _) = setup()

      movieWriteService ! UpdateMovie(movie1, probe.ref)
      probe.expectMessage(UpdateMovieSuccess(Movie("1", "Test Movie", 2023, 1)))
      persistenceTestKit.expectNextPersistedType[MovieUpdated]("MovieActor").movie shouldBe movie1
    }

    "update a movie if it doesn't exist" in {
      val (probe, movieWriteService, movie1, _, _) = setup()

      movieWriteService ! UpdateMovie(movie1, probe.ref)
      probe.expectMessage(UpdateMovieSuccess(Movie("1", "Test Movie", 2023, 1)))
      persistenceTestKit.expectNextPersistedType[MovieUpdated]("MovieActor").movie shouldBe movie1
    }

    "not add a movie with an invalid rating" in {
      val (probe, movieWriteService, movie1, _, _) = setup()

      val movieWithInvalidRating = movie1.copy(rating = 4)

      movieWriteService ! AddMovie(movieWithInvalidRating, probe.ref)
      probe.expectMessage(AddMovieFailure(s"Rating [${movieWithInvalidRating.rating}] is not valid."))
      persistenceTestKit.expectNothingPersisted("MovieActor")
    }

    "not update a movie with an invalid rating" in {
      val (probe, movieWriteService, movie1, _, _) = setup()

      movieWriteService ! AddMovie(movie1, probe.ref)
      probe.expectMessage(AddMovieSuccess(movie1))
      persistenceTestKit.expectNextPersistedType[MovieAdded]("MovieActor").movie shouldBe movie1

      val updatedMovieWithInvalidRating = movie1.copy(rating = 6)

      movieWriteService ! UpdateMovie(updatedMovieWithInvalidRating, probe.ref)
      probe.expectMessage(
        UpdateMovieFailure(
          s"Rating [${updatedMovieWithInvalidRating.rating}] for movie [${updatedMovieWithInvalidRating.id}] is not valid."
        )
      )
      persistenceTestKit.expectNothingPersisted("MovieActor")
    }
  }
}
