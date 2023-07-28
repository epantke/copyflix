package encoflix.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import encoflix.domain.MovieDomain.{Movie, mapper}
import encoflix.protocol.MovieEvent._
import encoflix.routes.MovieRoutes.timeout
import encoflix.service.MovieReadActor
import encoflix.service.MovieReadActor.Protocol._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MovieListRouteTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  val movie1: Movie = Movie("1", "Test Movie", 2023, 1)
  val movie2: Movie = Movie("2", "Test Movie 2", 2024, 2)
  val movie3: Movie = Movie("3", "Test Movie 3", 2025, 3)
  val movies: Seq[Movie] = Seq(movie1, movie2, movie3)

  def beforeEach(): Route = {
    val testKit = ActorTestKit(
      "MovieListRouteTest",
      EventSourcedBehaviorTestKit.config
    )
    implicit val typedSystem: ActorSystem[_] = testKit.system

    val mockReadJournal: TestReadJournal = new TestReadJournal(
      Seq(MovieAdded(movie1), MovieAdded(movie2), MovieAdded(movie3))
    )

    val movieReadActor: ActorRef[ActorCommand] = {
      testKit.spawn(MovieReadActor(PersistenceId.ofUniqueId("MovieListRouteTest"), mockReadJournal))
    }
    pathPrefix("movies") { MovieListRoute(movieReadActor).route }
  }

  "return all movies for GET requests to /movies" in {
    val movieRoute = beforeEach()

    Get("/movies") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
      contentType shouldEqual ContentTypes.`application/json`
      entityAs[Seq[Movie]] shouldEqual movies
    }
  }

  "return all movies for a specific year for GET requests to /movies?year={year}" in {
    val movieRoute = beforeEach()

    val year = 2024
    val moviesInYear = Seq(movie2)

    Get(s"/movies?year=$year") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
      contentType shouldEqual ContentTypes.`application/json`
      entityAs[Seq[Movie]] shouldEqual moviesInYear
    }
  }

  "return empty list for specific year for GET requests to /movies?year={year}" in {
    val movieRoute = beforeEach()

    val year = 9999

    Get(s"/movies?year=$year") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
      contentType shouldEqual ContentTypes.`application/json`
      entityAs[Seq[Movie]] shouldEqual Seq.empty
    }
  }
}
