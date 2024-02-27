/**
 * ServiceMail.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.notification.service;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.NotificationMessage;
import de.dlr.proseo.notification.NotificationConfiguration;

/**
 * The mail service to send emails using JavaMail. It provides methods to send plain text or MIME (HTML) emails.
 * 
 * @author Ernst Melchinger
 */
@Service
public class ServiceMail {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ServiceMail.class);

	/** The configuration of the notification service */
	@Autowired
	NotificationConfiguration config;

	/**
	 * Creates and configures a JavaMailSenderImpl instance with the necessary properties, and reads the mail-related configuration
	 * from the NotificationConfiguration.
	 * 
	 * @return The JavaMailSenderImpl instance
	 */
	public JavaMailSenderImpl getMailSender() {
		if (logger.isTraceEnabled())
			logger.trace(">>> getMailSender()");

		// Create and configure a new instance of JavaMailSenderImpl that provides functionality to send emails
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(config.getMailHost());
		mailSender.setPort(config.getMailPort().intValue());
		mailSender.setUsername(config.getMailUser());
		mailSender.setPassword(config.getMailPassword());

		// Create and configure a new instance of Properties to hold mail-related properties
		Properties properties = new Properties();
		properties.setProperty("mail.smtp.auth", config.getMailAuth().toString());
		properties.setProperty("mail.smtp.starttls.enable", config.getMailStarttls().toString());
		properties.setProperty("properties.mail.smtp.connectiontimeout", config.getMailTimeout().toString());
		properties.setProperty("properties.mail.smtp.timeout", config.getMailTimeout().toString());
		properties.setProperty("properties.mail.smtp.writetimeout", config.getMailTimeout().toString());

		mailSender.setJavaMailProperties(properties);
		return mailSender;
	}

	/**
	 * Sends an email based on the provided parameters. The email can be sent as plain text or MIME (HTML) format.
	 * 
	 * @param endpoint    The email address
	 * @param subject     The email subject
	 * @param mediaType   The media type to send the email
	 * @param messageCode The message code
	 * @param message     The email body
	 * @param sender      The email sender
	 * @throws IllegalArgumentException if an error occurs during mail preparation
	 * @throws MailException if an error occurs during mail sending
	 */
	public void sendMail(String endpoint, String subject, MediaType mediaType, String messageCode, String message,
			String sender) throws IllegalArgumentException, MailException {
		if (logger.isTraceEnabled())
			logger.trace(">>> sendMail({}, {}, {}, {}, {}, {})", endpoint, subject, mediaType,
					messageCode, message, sender);

		// Set the endpoint
		String to = endpoint;
		if (endpoint != null) {
			if (endpoint.toLowerCase().startsWith("mailto:")) {
				to = endpoint.toLowerCase().replace("mailto:", "");
				to = to.replaceAll("/", "");
			}
		}

		// Send the message according to media type
		if (mediaType.equals(MediaType.TEXT_PLAIN)) {
			sendSimpleMessage(to, subject, messageCode, message, sender);
		} else if (mediaType.equals(MediaType.APPLICATION_JSON)) {
			sendSimpleMessage(to, subject, messageCode, message, sender);
		} else if (mediaType.equals(MediaType.TEXT_HTML)) {
			sendMimeMessage(to, subject, messageCode, message, sender);
		} else {
			logger.log(NotificationMessage.UNKNOWN_MEDIATYPE, mediaType);
			sendSimpleMessage(to, subject, messageCode, message, sender);
		}
	}

	/**
	 * Send a plain text email based on the provided parameters.
	 * 
	 * @param to          The email address
	 * @param subject     The email subject
	 * @param messageCode The message code
	 * @param message     The email body
	 * @param sender      The email sender
	 * @throws MailException If an error occurs during mail sending
	 */
	public void sendSimpleMessage(String to, String subject, String messageCode, String message, String sender) throws MailException {
		if (logger.isTraceEnabled())
			logger.trace(">>> sendSimpleMessage({})", to);

		SimpleMailMessage newMsg = new SimpleMailMessage();

		newMsg.setFrom(sender);
		newMsg.setTo(to);
		newMsg.setSubject(subject);
		newMsg.setText(message);

		getMailSender().send(newMsg);
	}

	/**
	 * Send an HTML email as a MIME message based on the provided parameters.
	 * 
	 * @param to          The email address
	 * @param subject     The email subject
	 * @param messageCode The message code
	 * @param message     The email body
	 * @param sender      The email sender
	 * @throws IllegalArgumentException if an error occurs during mail preparation
	 * @throws MailException if an error occurs during mail sending
	 */
	public void sendMimeMessage(String to, String subject, String messageCode, String message, String sender) throws IllegalArgumentException, MailException {
		if (logger.isTraceEnabled())
			logger.trace(">>> sendMimeMessage({})", to);

		MimeMessage newMsg = getMailSender().createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(newMsg);

		try {
			helper.setSubject(subject);
			helper.setFrom(sender);
			helper.setTo(to);

			boolean html = true;
			helper.setText(message, html);
			getMailSender().send(newMsg);
		} catch (MessagingException e) {
			String msg = logger.log(NotificationMessage.MESSAGING_EXCEPTION, e.getMessage());
			throw new IllegalArgumentException(msg);
		}
	}

}