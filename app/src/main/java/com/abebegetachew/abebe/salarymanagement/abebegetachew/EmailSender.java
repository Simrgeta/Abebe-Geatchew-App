package com.abebegetachew.abebe.salarymanagement.abebegetachew;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    private final String username = "simrgetaawash.app@gmail.com"; // Your Gmail
    private final String password = "zxofhdshfuyavwst";             // App Password

    public void sendEmail(String recipientEmail, String subject, String messageBody) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        // Force correct Authenticator use
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                System.out.println("DEBUG: Using correct authenticator!");
                return new PasswordAuthentication(username, password);
            }
        });

        session.setDebug(true); // Log SMTP details

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject(subject);
        message.setText(messageBody);

        Transport.send(message);
        System.out.println("DEBUG: Email sent to " + recipientEmail);
    }
}
