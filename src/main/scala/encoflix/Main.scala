package encoflix

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.contrib.persistence.mongodb.{MongoReadJournal, ScalaDslMongoReadJournal}
import akka.persistence.query.PersistenceQuery
import akka.persistence.typed.PersistenceId
import com.typesafe.config.{Config, ConfigFactory}
import encoflix.routes.MovieRoutes
import encoflix.service.{HttpServerActor, MovieReadActor, MovieWriteActor}

import scala.concurrent.duration.DurationLong

object Main {
  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[NotUsed] { context =>
      implicit val scheduler: Scheduler = context.system.scheduler
      val loadedConfig = context.system.settings.config.getConfig("encoflix")
      val supervisorStrategy = getSupervisorStrategy(loadedConfig)

      val readJournal: ScalaDslMongoReadJournal = PersistenceQuery(context.system)
        .readJournalFor[ScalaDslMongoReadJournal](MongoReadJournal.Identifier)

      val persistenceId = PersistenceId.ofUniqueId("MovieActor")

      val movieReadActor =
        createSupervisedActor(context, MovieReadActor(persistenceId, readJournal), "MovieReadActor", supervisorStrategy)

      val movieWriteActor = createSupervisedActor(
        context,
        MovieWriteActor(persistenceId, supervisorStrategy),
        "MovieWriteActor",
        supervisorStrategy
      )

      createSupervisedActor(
        context,
        HttpServerActor(MovieRoutes(movieWriteActor, movieReadActor)(scheduler)),
        "HttpServerActor",
        supervisorStrategy
      )

      Behaviors.empty
    }

    ActorSystem[NotUsed](rootBehavior, "MovieSystem", ConfigFactory.load())
  }

  private def createSupervisedActor[T](
      context: ActorContext[NotUsed],
      behavior: Behavior[T],
      name: String,
      supervisorStrategy: BackoffSupervisorStrategy
  ): ActorRef[T] = {
    context.spawn(
      Behaviors
        .supervise(behavior)
        .onFailure[Exception](supervisorStrategy),
      name
    )
  }

  private def getSupervisorStrategy(loadedConfig: Config): BackoffSupervisorStrategy = SupervisorStrategy
    .restartWithBackoff(
      loadedConfig.getDuration("minBackoff").toMillis.millis,
      loadedConfig.getDuration("maxBackoff").toMillis.millis,
      loadedConfig.getDouble("randomFactor")
    )
    .withMaxRestarts(loadedConfig.getInt("maxRestarts"))
    .withResetBackoffAfter(loadedConfig.getDuration("resetBackoffAfter").toMillis.millis)
}
