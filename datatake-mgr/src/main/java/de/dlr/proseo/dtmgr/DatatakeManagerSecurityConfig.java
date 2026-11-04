/**
 * DatatakeManagerSecurityConfig.java
 * 
 * (c) 2026 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.dtmgr;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security configuration for CProS Sentinel-1 Mission Planning Monitor
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class DatatakeManagerSecurityConfig {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(DatatakeManagerSecurityConfig.class);
	
	/**
	 * Set the Ingestor security options
	 * 
	 * @param http the HTTP security object
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		String base ="/proseo/processor-mgr/v0.1";
		http.httpBasic(it -> {})
			.authorizeHttpRequests(requests -> requests
			.requestMatchers("/actuator/health")
			.permitAll()
			.requestMatchers(HttpMethod.GET)
			.hasAnyRole(UserRole.ORDER_READER.toString())
			.anyRequest()
			.hasAnyRole(UserRole.PRODUCT_INGESTOR.toString()))
			.csrf((csrf) -> csrf.disable());

		return http.build();
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

	/**
	 * Provides the default user details service for prosEO (based on the standard data model for users and groups)
	 * 
	 * @return a JdbcDaoImpl object
	 */
	@Bean
	public UserDetailsService userDetailsService() {
		logger.log(GeneralMessage.INITIALIZING_USER_DETAILS_SERVICE, dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);
		
		return jdbcDaoImpl;
	}
}
