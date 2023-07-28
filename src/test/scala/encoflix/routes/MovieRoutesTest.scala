package encoflix.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.{ActorRef, ActorSystem, SupervisorStrategy}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import encoflix.protocol.MovieProtocol._
import encoflix.service.MovieReadActor.Protocol.ActorCommand
import encoflix.service.{MovieReadActor, MovieWriteActor}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt

class MovieRoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  def beforeEach(): Route = {
    val testKit = ActorTestKit(
      "AddMovieRouteTest",
      EventSourcedBehaviorTestKit.config
    )
    implicit val typedSystem: ActorSystem[_] = testKit.system

    val mockReadJournal: TestReadJournal = new TestReadJournal(Seq.empty)

    val movieReadActor: ActorRef[ActorCommand] =
      testKit.spawn(MovieReadActor(PersistenceId.ofUniqueId("MovieRoutesTest"), mockReadJournal))
    val movieWriteActor: ActorRef[Command] = testKit.spawn(
      MovieWriteActor(
        PersistenceId.ofUniqueId("MovieListRouteTest"),
        SupervisorStrategy.restartWithBackoff(1.second, 5.seconds, 0.1)
      )
    )
    MovieRoutes(movieWriteActor, movieReadActor)
  }

  "return a 400 status for POST requests to /movies if the movie data is invalid JSON data" in {
    val movieRoutes = beforeEach()

    val invalidMovieData = "{\"bla\":[]}"

    Post("/movies", HttpEntity(ContentTypes.`application/json`, invalidMovieData)) ~> movieRoutes ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  "return a 400 status for POST requests to /movies if the movie data is invalid data" in {
    val movieRoutes = beforeEach()

    val invalidMovieData = "!ä1_4%24'`qdw*@##efef"

    Post("/movies", HttpEntity(ContentTypes.`application/json`, invalidMovieData)) ~> movieRoutes ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }

  "return a 404 status for GET requests to /movies/{year} if the year data is invalid and not JSON" in {
    val movieRoutes = beforeEach()

    val nonNumericYear = "year"

    Get(s"/movies/$nonNumericYear") ~> movieRoutes ~> check {
      status shouldEqual StatusCodes.NotFound
    }
  }

  "return a 400 status for GET requests to /movies?year={year} if year is invalid" in {
    val movieRoutes = beforeEach()

    val invalidYear = "invalid"

    Get(s"/movies?year=$invalidYear") ~> movieRoutes ~> check {
      status shouldEqual StatusCodes.BadRequest
    }
  }
}
