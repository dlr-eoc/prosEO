package de.dlr.proseo.notification.service;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.NotificationMessage;
import de.dlr.proseo.notification.NotificationConfiguration;

/**
 * The mail service to send the message
 * 
 * @author Ernst Melchinger
 *
 */
@Service
public class ServiceMail {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ServiceConnection.class);

	@Autowired
	private NotificationConfiguration config;
	
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.NOTIFICATION);

	/**
	 * Create and initialize a new java mail sender instance
	 *  
	 * @return The java mail sender instance
	 */
	public JavaMailSenderImpl getMailSender() {
		if (logger.isTraceEnabled()) logger.trace(">>> getMailSender()");
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(config.getMailHost());
		mailSender.setPort(config.getMailPort().intValue());
		mailSender.setUsername(config.getMailUser());
		mailSender.setPassword(config.getMailPassword());

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
	 * Send a mail
	 * 
	 * @param endpoint The mail address
	 * @param user The user name for basic HTTP authentication (optional)
	 * @param password The password for basic HTTP authentication (optional)
	 * @param subject The message subject
	 * @param mediaType The media type to send the message
	 * @param messageCode The message code
	 * @param message The message body
	 * @param sender The message sender
	 * @return The response entity
	 */
	public ResponseEntity<?> sendMail(String endpoint, String user, String password, String subject, 
			MediaType mediaType, String messageCode, String message, String sender) {
		if (logger.isTraceEnabled()) logger.trace(">>> sendMail({}, {})", endpoint, message);
		String to = endpoint;
		if (endpoint != null) {
			if (endpoint.toLowerCase().startsWith("mailto:")) {
				to = endpoint.toLowerCase().replace("mailto:", "");
				to = to.replaceAll("/", "");
			}
		}
		if (mediaType.equals(MediaType.TEXT_PLAIN)) {
			return sendSimpleMessage(to, user, password, subject, messageCode, message, sender);
		} else if (mediaType.equals(MediaType.TEXT_HTML)) {
			return sendMimeMessage(to, user, password, subject, messageCode, message, sender);
		}
		// error handling
		return null;
		
	}

	/**
	 * Send a mail as plain text message
	 * 
	 * @param endpoint The mail address
	 * @param user The user name for basic HTTP authentication (optional)
	 * @param password The password for basic HTTP authentication (optional)
	 * @param subject The message subject
	 * @param messageCode The message code
	 * @param message The message body
	 * @param sender The message sender
	 * @return The response entity
	 */
	public ResponseEntity<?> sendSimpleMessage(String to, String user, String password, String subject, 
			String messageCode, String message, String sender) {
		if (logger.isTraceEnabled()) logger.trace(">>> sendSimpleMessage({})", to);
		SimpleMailMessage newMsg = new SimpleMailMessage();

		newMsg.setFrom(sender);
		newMsg.setTo(to);
		newMsg.setSubject(subject);
		newMsg.setText(message);

		getMailSender().send(newMsg);
		return null;
	}

	/**
	 * Send a mail as mime (HTTP) message
	 * 
	 * @param endpoint The mail address
	 * @param user The user name for basic HTTP authentication (optional)
	 * @param password The password for basic HTTP authentication (optional)
	 * @param subject The message subject
	 * @param messageCode The message code
	 * @param message The message body
	 * @param sender The message sender
	 * @return The response entity
	 */
	public ResponseEntity<?> sendMimeMessage(String to, String user, String password, String subject, 
			String messageCode, String message, String sender) {
		if (logger.isTraceEnabled()) logger.trace(">>> sendMimeMessage({})", to);
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
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
		} catch (MailException e) {
			String msg = logger.log(NotificationMessage.MESSAGING_EXCEPTION, e.getMessage());
			return new ResponseEntity<>(http.errorHeaders(msg), HttpStatus.BAD_REQUEST);
		}
		return null;		
	}
}
