package de.dlr.proseo.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO NotificationService component
 * 
 * @author Ernst Melchinger
 *
 */
@Configuration
@ConfigurationProperties(prefix = "proseo.monitor")
public class NotificationConfiguration {
	/**
	 * Default subject
	 */
	private String subject;
	/**
	 * Default HTML content type
	 */
	private String contentType;
	/**
	 * Default sender
	 */
	private String sender;	
	/**
	 * Default mail sender
	 */
	@Value("${proseo.notification.mail.sender}")
	private String mailSender;	
	/**
	 * Mail smtp host
	 */
	@Value("${proseo.notification.mail.host}")
	private String mailHost;
	/**
	 * Mail port
	 */
	@Value("${proseo.notification.mail.port}")
	private Long mailPort;
	/**
	 * Mail user
	 */
	@Value("${proseo.notification.mail.user}")
	private String mailUser;
	/**
	 * Mail password
	 */
	@Value("${proseo.notification.mail.password}")
	private String mailPassword;
	/**
	 * Use authentication
	 */
	@Value("${proseo.notification.mail.auth}")
	private Boolean mailAuth;
	/**
	 * Use starttls
	 */
	@Value("${proseo.notification.mail.starttls}")
	private Boolean mailStarttls;
	/**
	 * Mail timeout
	 */
	@Value("${proseo.notification.mail.timeout}")
	private Long mailTimeout;
	/** 
	 * Timeout for HTTP connections 
	 */
	@Value("${proseo.http.timeout}")
	private Long httpTimeout;
	
	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}
	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}
	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}
	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	/**
	 * @return the sender
	 */
	public String getSender() {
		return sender;
	}
	/**
	 * @param sender the sender to set
	 */
	public void setSender(String sender) {
		this.sender = sender;
	}
	/**
	 * @return the httpTimeout
	 */
	public Long getHttpTimeout() {
		return httpTimeout;
	}
	/**
	 * @param httpTimeout the httpTimeout to set
	 */
	public void setHttpTimeout(Long httpTimeout) {
		this.httpTimeout = httpTimeout;
	}
	/**
	 * @return the mailSender
	 */
	public String getMailSender() {
		return mailSender;
	}
	/**
	 * @return the mailHost
	 */
	public String getMailHost() {
		return mailHost;
	}
	/**
	 * @return the mailPort
	 */
	public Long getMailPort() {
		return mailPort;
	}
	/**
	 * @return the mailUser
	 */
	public String getMailUser() {
		return mailUser;
	}
	/**
	 * @return the mailPassword
	 */
	public String getMailPassword() {
		return mailPassword;
	}
	/**
	 * @return the mailAuth
	 */
	public Boolean getMailAuth() {
		return mailAuth;
	}
	/**
	 * @return the mailStarttls
	 */
	public Boolean getMailStarttls() {
		return mailStarttls;
	}
	/**
	 * @return the mailTimeout
	 */
	public Long getMailTimeout() {
		return mailTimeout;
	}
	/**
	 * @param mailSender the mailSender to set
	 */
	public void setMailSender(String mailSender) {
		this.mailSender = mailSender;
	}
	/**
	 * @param mailHost the mailHost to set
	 */
	public void setMailHost(String mailHost) {
		this.mailHost = mailHost;
	}
	/**
	 * @param mailPort the mailPort to set
	 */
	public void setMailPort(Long mailPort) {
		this.mailPort = mailPort;
	}
	/**
	 * @param mailUser the mailUser to set
	 */
	public void setMailUser(String mailUser) {
		this.mailUser = mailUser;
	}
	/**
	 * @param mailPassword the mailPassword to set
	 */
	public void setMailPassword(String mailPassword) {
		this.mailPassword = mailPassword;
	}
	/**
	 * @param mailAuth the mailAuth to set
	 */
	public void setMailAuth(Boolean mailAuth) {
		this.mailAuth = mailAuth;
	}
	/**
	 * @param mailStarttls the mailStarttls to set
	 */
	public void setMailStarttls(Boolean mailStarttls) {
		this.mailStarttls = mailStarttls;
	}
	/**
	 * @param mailTimeout the mailTimeout to set
	 */
	public void setMailTimeout(Long mailTimeout) {
		this.mailTimeout = mailTimeout;
	}
	
	

}
