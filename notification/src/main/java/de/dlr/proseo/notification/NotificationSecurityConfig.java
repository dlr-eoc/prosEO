package de.dlr.proseo.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;

@Configuration
@EnableWebSecurity
public class NotificationSecurityConfig extends WebSecurityConfigurerAdapter {
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(NotificationSecurityConfig.class);
	
	/**
	 * Set the Ingestor security options
	 * 
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
//			.httpBasic()
//				.and()
			.authorizeRequests()
			.antMatchers("/**/notify").permitAll()
			.antMatchers("/**/actuator/health").permitAll()
			.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Provides the default password encoder for prosEO (BCrypt)
	 * 
	 * @return a BCryptPasswordEncoder
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}
}


