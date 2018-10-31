import Payment._
import akka.actor.{ActorRef, FSM}

object Payment {
  sealed trait PaymentState
  case object Open extends PaymentState

  sealed trait PaymentCommand
  case object DoPayment

  sealed trait PaymentEvent
  case object PaymentReceived extends PaymentEvent
  case object PaymentConfirmed extends PaymentEvent

  case class PaymentServiceStarted(paymentRef: ActorRef)

  sealed trait PaymentData
}

class Payment extends FSM[PaymentState, PaymentData] {

  startWith(Open, null)

  when(Open) {
    case Event(DoPayment, _) =>
      log.info("Received Payment")
      sender ! PaymentConfirmed
      context.parent ! PaymentReceived
      stop
      stay
  }

  initialize()
}
