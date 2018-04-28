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

trait Routes extends Directives {

  case class SessionPayload(sessionId: String, serviceCode: String, phoneNumber: String, text: String)

  case class RegisteredUser(id: String, phoneNumber: String, name: String, paid: Boolean = false)

  var users: Map[String, RegisteredUser] = Map.empty

  val userRoutes =
    path("ussd") {
      post {
        formFields('sessionId, 'serviceCode, 'phoneNumber, 'text).as(SessionPayload) { msg: SessionPayload =>
          if (msg.text.isEmpty) {
            complete("""CON 
                Welcome to LSConf2018.
                Please enter your name:
                """)
          } else {
            if (msg.text.filter(_ != '*').isEmpty) {
              complete("CON Did you forget your name?")
            } else {
              if (msg.text.equals("2")) {
                if (users.contains(msg.phoneNumber)) {
                  val card = PaymentCard(
                    number = "5399832695266989",
                    cvvNumber = 597,
                    expiryMonth = 10,
                    expiryYear = 2020,
                    countryCode = "NG",
                    authToken = "0000")

                  val cardCheckout = CardCheckoutRequest(
                    productName = "LSConf2018",
                    currencyCode = CurrencyCode.NGN,
                    amount = 500.00,
                    cardDetails = card,
                    narration = "Payment for Lagos Scala Conference")

                  val response = PaymentService
                    .cardCheckout(cardCheckout)
                    .map { _ =>
                      "END Thanks for your payment :)"
                    } recover { case _: Throwable => "END Yawa don gas o." }
                  complete(response)

                } else {
                  val message = """CON 
                            Welcome to LSConf2018.
                            Please enter your name:
                            """
                  complete(message)
                }
              } else {
                val user = RegisteredUser(UUID.randomUUID.toString, msg.phoneNumber, msg.text)
                users += (msg.phoneNumber -> user)
                val message = Message(
                  text = s"Your conference registration code is ${user.id}",
                  recipients = List(msg.phoneNumber))
                val response = SmsService.send(message).map { _ =>
                  "END Thanks for registering. Your should receive your ID by SMS."
                } recover { case _: Throwable => "END Yawa don gas o." }
                complete(response)
              }
            }
          }
        }
      }
    }

  def cardPayment(): Unit = {
    val card = PaymentCard(
      number = "5399832695266989",
      cvvNumber = 597,
      expiryMonth = 10,
      expiryYear = 2020,
      countryCode = "NG",
      authToken = "0000")

    val cardCheckout = CardCheckoutRequest(
      productName = "LSConf2018",
      currencyCode = CurrencyCode.NGN,
      amount = 500.00,
      cardDetails = card,
      narration = "Payment for Lagos Scala Conference")

    PaymentService
      .cardCheckout(cardCheckout)
  }
}
