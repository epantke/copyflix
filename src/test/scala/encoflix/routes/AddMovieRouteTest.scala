package encoflix.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.actor.typed.{ActorRef, ActorSystem, SupervisorStrategy}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import akka.persistence.typed.PersistenceId
import de.heikoseeberger.akkahttpjackson.JacksonSupport._
import encoflix.domain.MovieDomain.{Movie, mapper}
import encoflix.protocol.MovieProtocol.{AddMovie, Command, Response}
import encoflix.routes.MovieRoutes.timeout
import encoflix.service.MovieWriteActor
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertLongToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec

class AddMovieRouteTest extends AnyWordSpec with Matchers with ScalatestRouteTest {

  val movie1: Movie = Movie("1", "Test Movie", 2023, 1)

  def beforeEach(): Route = {
    val testKit = ActorTestKit(
      "AddMovieRouteTest",
      EventSourcedBehaviorTestKit.config
    )
    implicit val typedSystem: ActorSystem[_] = testKit.system
    val movieActor: ActorRef[Command] = {
      testKit.spawn(
        MovieWriteActor(
          PersistenceId.ofUniqueId("AddMovieRouteTest"),
          SupervisorStrategy.restartWithBackoff(1.second, 5.seconds, 0.1)
        )
      )
    }
    val movieActorRef = testKit.createTestProbe[Response]().ref
    movieActor ! AddMovie(movie1, movieActorRef)
    pathPrefix("movies") { AddMovieRoute(movieActor).route }
  }

  "add a new movie for POST requests to /movies" in {
    val movieRoute = beforeEach()

    val newMovie = Movie("99", "New Movie", 2003, 2)

    Post("/movies", newMovie) ~> movieRoute ~> check {
      status shouldEqual StatusCodes.Created
      contentType shouldEqual ContentTypes.`application/json`
      entityAs[Movie] shouldEqual newMovie
    }
  }

  "return a 201 status for POST requests to /movies if the movie id already exists" in {
    val movieRoute = beforeEach()

    val duplicateMovie = movie1

    Post("/movies", duplicateMovie) ~> movieRoute ~> check {
      status shouldEqual StatusCodes.Created
    }
  }
}
