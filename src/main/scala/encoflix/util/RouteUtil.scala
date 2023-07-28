package encoflix.util

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import encoflix.domain.MovieDomain.mapper

object RouteUtil {
  def completeCreated[A](entity: A): Route = {
    complete(createHttpResponse(StatusCodes.Created, entity))
  }

  def completeBadRequest[A](entity: A): Route = {
    complete(createHttpResponse(StatusCodes.BadRequest, entity))
  }

  def completeOk[A](entity: A): Route = {
    complete(createHttpResponse(StatusCodes.OK, entity))
  }

  def completeNotFound[A](entity: A): Route = {
    complete(createHttpResponse(StatusCodes.NotFound, entity))
  }

  private def createHttpResponse[A](statusCode: StatusCode, entity: A): HttpResponse = {
    HttpResponse(statusCode, entity = HttpEntity(ContentTypes.`application/json`, mapper.writeValueAsString(entity)))
  }
}
