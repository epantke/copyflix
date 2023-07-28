package encoflix.service

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

object HttpServerActor {

  private sealed trait ServerState
  private case object Starting extends ServerState
  private case object Running extends ServerState

  sealed trait ServerResult
  private case object Started extends ServerResult
  private case class Failed(exMsg: String) extends ServerResult

  def apply(routes: Route): Behavior[ServerResult] =
    httpBehavior(Starting, routes)

  private def httpBehavior(state: ServerState, routes: Route): Behavior[ServerResult] = state match {
    case Starting =>
      Behaviors.setup[ServerResult] { context =>
        implicit val system: ActorSystem[Nothing] = context.system
        val config = system.settings.config.getConfig("encoflix")
        val address = config.getString("address")
        val port = config.getInt("port")

        val serverStarted = Http().newServerAt(address, port).bind(routes)
        context.pipeToSelf(serverStarted) {
          case Success(_) => Started
          case Failure(ex) => Failed(ex.getMessage)
        }
        Behaviors.receiveMessage({
          case Started =>
            context.log.info("HTTP server started on {}:{}!", address, port)
            httpBehavior(Running, routes)
          case Failed(exMsg) =>
            context.log.error("Failed to start HTTP server: ", exMsg)
            Behaviors.stopped
        })
      }
    case Running =>
      Behaviors.receiveSignal { case (context, exception: Exception) =>
        context.system.log.error("HTTP server failed, restarting: ", exception)
        httpBehavior(Starting, routes)
      }
  }
}
