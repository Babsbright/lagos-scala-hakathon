package com.lagosscala.hakathon

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import com.africastalking.sms._
import com.africastalking.payment._
import com.africastalking.core.utils.CurrencyCode
import com.africastalking.sms.request.Message
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A custom extractor object so we can pattern-match against menu paths.
 *
 * AT's USSD API passes a 'text' parameter to our callback, which contains the
 * user's current path through our menu (all the input they've provided), separated
 * by '*'. This extractor deconstructs that into a 'Seq[String]', which we can bind
 * in our `case` patterns.
 *
 * To see how it's used, check out the {@see com.lagosscala.hakathon.Route} trait.
 *
 * More on extractors here: https://docs.scala-lang.org/tour/extractor-objects.html
 */
object menuPath {
  def unapplySeq(input: String): Option[Seq[String]] = {
    val parts = input.trim.split('*')
    if (parts.isEmpty) None else Some(parts)
  }
}

trait Routes extends Directives {

  final val AUTH_TOKEN = "0000"
  final val CONFERENCE_NAME = "LSConf2018"
  final val COUNTRY_CODE = "NG"

  case class SessionPayload(sessionId: String, serviceCode: String, phoneNumber: String, text: String)

  case class RegisteredUser(id: String, phoneNumber: String, name: String, paid: Boolean = false)

  var users: Map[String, RegisteredUser] = Map.empty

  val userRoutes =
    path("ussd") {
      post {
        formFields('sessionId, 'serviceCode, 'phoneNumber, 'text).as(SessionPayload) { msg: SessionPayload =>
          msg.text match {
            case menuPath("1") =>
              complete("CON Please enter your name:")
            case menuPath("1", name) if name.isEmpty =>
              complete("CON Did you forget your name?")
            case menuPath("1", name) =>
              val user = RegisteredUser(UUID.randomUUID.toString, msg.phoneNumber, name)
              val response = sendSMS(user)
              users += (msg.phoneNumber -> user)
              complete(response)
            case menuPath("2") =>
              complete("CON Please enter your 16-digit card number:")
            case menuPath("2", _) =>
              complete("CON Please enter your CVV (check the back of your card):")
            case menuPath("2", _, _) =>
              complete("""CON 
                Enter card expiry month and year, separated by /
                E.g. 10/2020 for October 2020:""")
            case menuPath("2", cardNumber, cvv, expiry) =>
              val response = makePayment(cardNumber, cvv, expiry)
              complete(response)
            case _ =>
              complete(s"""CON 
                Welcome to ${CONFERENCE_NAME}.
                1. Register
                2. Pay for your seat
                Choose an option:""")
          }
        }
      }
    }

  def sendSMS(user: RegisteredUser) = {
    val message = Message(
      text = s"Thanks ${user.name}. Your registration ID is ${user.id}",
      recipients = List(user.phoneNumber)
    )
    SmsService
      .send(message)
      .map { _ =>
        "END Thanks for registering. You should receive your ID by SMS."
      } recover { case _: Throwable => "END Yawa don gas o." }
  }

  def makePayment(cardNumber: String, cvv: String, expiry: String) = {
    val Array(expiryMonth, expiryYear) = expiry.split('/')
    val card = PaymentCard(
      number = cardNumber,
      cvvNumber = cvv.toInt,
      expiryMonth = expiryMonth.toInt,
      expiryYear = expiryYear.toInt,
      countryCode = COUNTRY_CODE,
      authToken = AUTH_TOKEN
    )
    val cardCheckout = CardCheckoutRequest(
      productName = CONFERENCE_NAME,
      currencyCode = CurrencyCode.NGN,
      amount = 500,
      cardDetails = card,
      narration = "Payment for Lagos Scala Conference"
    )
    PaymentService
      .cardCheckout(cardCheckout)
      .map { _ =>
        "END Thanks for your payment :)"
      } recover {
        case _: Throwable => "END Yawa don gas o."
      }
  }
}
