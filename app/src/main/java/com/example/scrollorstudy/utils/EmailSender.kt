package com.example.scrollorstudy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import android.util.Log

object EmailSender {

    // TODO: USER MANUAL ACTION REQUIRED
    // 1. Go to your Google Account Settings -> Security -> 2-Step Verification
    // 2. Create an App Password for "Mail"
    // 3. Paste your Gmail address and the 16-character App Password below.
    private const val SENDER_EMAIL = "sailokeshnalabothu7@gmail.com"
    private const val SENDER_PASSWORD = "pypgzzsiqrcwmtuq" 
    
    suspend fun sendOtpEmail(recipientEmail: String, otp: String, parentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "Scroll Or Study"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    subject = "Your Verification Code - Scroll Or Study"
                    
                    val htmlContent = """
                        <h3>Welcome to Scroll Or Study!</h3>
                        <p>Your verification code is: <strong>$otp</strong></p>
                        <hr>
                        <h4>Parent Monitoring Details:</h4>
                        <p><strong>Parent ID:</strong> $parentId</p>
                        <p><strong>Default Parent Password:</strong> welcome</p>
                        <br>
                        <p>Give these details to your parents so they can access the Monitoring Dashboard.</p>
                    """.trimIndent()
                    
                    setContent(htmlContent, "text/html; charset=utf-8")
                }

                Transport.send(message)
                Log.d("EmailSender", "OTP Email sent successfully")
                true
            } catch (e: Exception) {
                Log.e("EmailSender", "Failed to send email", e)
                false
            }
        }
    }

    suspend fun sendWelcomeEmail(recipientEmail: String, parentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "Scroll Or Study"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    subject = "Welcome to Scroll Or Study"

                    val htmlContent = """
                        <h3>Welcome to Scroll Or Study!</h3>
                        <p>Thank you for logging in with Google.</p>
                        <hr>
                        <h4>Parent Monitoring Details:</h4>
                        <p><strong>Parent ID:</strong> $parentId</p>
                        <p><strong>Default Parent Password:</strong> welcome</p>
                        <br>
                        <p>Give these details to your parents so they can access the Monitoring Dashboard.</p>
                    """.trimIndent()

                    setContent(htmlContent, "text/html; charset=utf-8")
                }

                Transport.send(message)
                Log.d("EmailSender", "Welcome Email sent successfully")
                true
            } catch (e: Exception) {
                Log.e("EmailSender", "Failed to send email", e)
                false
            }
        }
    }
}
