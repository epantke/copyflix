package encoflix.routes

import akka.NotUsed
import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, EventsByPersistenceIdQuery, ReadJournal}
import akka.persistence.query.{EventEnvelope, NoOffset}
import akka.stream.scaladsl.Source
import encoflix.protocol.MovieEvent.MovieEvent

class TestReadJournal(events: Seq[MovieEvent])
    extends ReadJournal
    with CurrentEventsByPersistenceIdQuery
    with EventsByPersistenceIdQuery {

  private val eventEnvelopes: Seq[EventEnvelope] = events.zipWithIndex.map { case (event, index) =>
    EventEnvelope(NoOffset, "MockPersistenceId", index.toLong, event, System.currentTimeMillis())
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
