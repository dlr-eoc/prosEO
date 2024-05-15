/**
 * NotificationSecurityConfig.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 
 * Configures the security settings for the notification system in a Java application using the Spring Security framework.
 * 
 * @author Ernst Melchinger
 */
@Configuration
@EnableWebSecurity
public class NotificationSecurityConfig {

	/**
	 * Configures the HTTP security for the application, defining which requests should be allowed or denied
	 *
	 * @param http the HTTP security object
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.httpBasic(it -> {})
		.authorizeHttpRequests(requests -> requests
				.antMatchers("/**/notify")
				.permitAll()
				.antMatchers("/**/actuator/health")
				.permitAll())
		.csrf((csrf) -> csrf.disable()); // Required for POST requests (or configure CSRF)
		return http.build();
	}

	/**
	 * Provides the default password encoder for prosEO (BCrypt) for hashing and verifying passwords
	 *
	 * @return a BCryptPasswordEncoder
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}