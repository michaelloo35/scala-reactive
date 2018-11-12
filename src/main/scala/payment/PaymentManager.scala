package payment

import akka.actor.ActorRef
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import order.OrderManager.{OrderCommand, OrderEvent}
import payment.PaymentManager.{DoPayment, Open, PaymentState, _}

import scala.reflect.{ClassTag, classTag}

object PaymentManager {

  sealed trait PaymentState extends FSMState
  case object Open extends PaymentState {
    override def identifier: String = "Open"
  }

  sealed trait PaymentCommand
  case object DoPayment extends PaymentCommand with OrderCommand with OrderEvent

  sealed trait PaymentEvent
  case object PaymentReceived extends PaymentEvent with OrderEvent
  case object PaymentConfirmed extends PaymentEvent with OrderEvent
  case class PaymentServiceStarted(paymentRef: ActorRef) extends PaymentEvent with OrderEvent

}

class PaymentManager extends PersistentFSM[PaymentState, Payment, PaymentEvent] {
  override def persistenceId: String = "payment-manager-fsm-id-1"
  override def domainEventClassTag: ClassTag[PaymentEvent] = classTag[PaymentEvent]
  startWith(Open, null)

  when(Open) {
    case Event(DoPayment, _) =>
      sender ! PaymentConfirmed
      context.parent ! PaymentReceived
      stop replying PaymentConfirmed
  }

  whenUnhandled {
    case Event(e, s) =>
      log.info("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }
  override def applyEvent(domainEvent: PaymentEvent, paymentBeforeEvent: Payment): Payment = paymentBeforeEvent

}
