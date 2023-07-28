package encoflix.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.{ActorRef, ActorSystem, SupervisorStrategy}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import encoflix.domain.MovieDomain.{Movie, mapper}
import encoflix.protocol.MovieEvent._
import encoflix.protocol.MovieProtocol._
import encoflix.routes.MovieRoutes.timeout
import encoflix.service.MovieReadActor.Protocol._
import encoflix.service.{MovieReadActor, MovieWriteActor}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec

class MovieDetailsRouteTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  val movie1: Movie = Movie("1", "Test Movie", 2023, 1)

  def beforeEach(): Route = {
    val testKit = ActorTestKit(
      "MovieDetailsRouteTest",
      EventSourcedBehaviorTestKit.config
    )

    implicit val typedSystem: ActorSystem[_] = testKit.system

    val movieWriteActor: ActorRef[Command] = testKit.spawn(
      MovieWriteActor(
        PersistenceId.ofUniqueId("MovieDetailsRouteTest"),
        SupervisorStrategy.restartWithBackoff(1.second, 5.seconds, 0.1)
      )
    )

    val mockReadJournal: TestReadJournal = new TestReadJournal(Seq(MovieAdded(movie1)))

    val movieReadActor: ActorRef[ActorCommand] =
      testKit.spawn(MovieReadActor(PersistenceId.ofUniqueId("MovieDetailsRouteTest"), mockReadJournal))

    pathPrefix("movies") { MovieDetailsRoute(movieWriteActor, movieReadActor).route }
  }

  "return a specific movie for GET requests to /movies/{id}" in {
    val movieRoute = beforeEach()

    Get("/movies/1") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
      contentType shouldEqual ContentTypes.`application/json`
      entityAs[Movie] shouldEqual movie1
    }
  }

  "return a 404 status for GET requests to /movies/{id} if the movie does not exist" in {
    val movieRoute = beforeEach()

    Get("/movies/999") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  "update a movie for PUT requests to /movies/{id}" in {
    val movieRoute = beforeEach()

    val updatedMovie = Movie("1", "Updated Test Movie", 2024, 3)

    Put("/movies/1", updatedMovie) ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
      contentType shouldEqual ContentTypes.`application/json`
      entityAs[Movie] shouldEqual updatedMovie
    }
  }

  "return a 200 status for PUT requests to /movies/{id} if the movie does not exist" in {
    val movieRoute = beforeEach()

    val updatedMovie = Movie("1", "Updated Test Movie", 2024, 3)

    Put("/movies/999", updatedMovie) ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "return a 200 status for DELETE requests to /movies/{id} if the movie does not exist" in {
    val movieRoute = beforeEach()

    val nonexistentId = "999"

    Delete(s"/movies/$nonexistentId") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.OK
    }
  }

  "return a 404 status for GET if the movie does not exist" in {
    val movieRoute = beforeEach()

    val nonexistentId = "999"

    Get(s"/movies/$nonexistentId") ~> movieRoute ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }
}
