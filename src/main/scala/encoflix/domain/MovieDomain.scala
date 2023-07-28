package encoflix.domain

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}

object MovieDomain {

  type MovieName = String
  type MovieYear = Int
  type MovieRating = Int
  type MovieId = String

  case class Movie(
      @JsonProperty("id") id: MovieId,
      @JsonProperty("name") name: MovieName,
      @JsonProperty("year") year: MovieYear,
      @JsonProperty("rating") rating: MovieRating
  )

  // (De-)serialization
  implicit val mapper: ObjectMapper = (new ObjectMapper() with ClassTagExtensions {}).registerModule(DefaultScalaModule)
}
