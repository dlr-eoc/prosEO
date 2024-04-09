/**
 * ProductionPlannerSecurityConfig.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner;

import java.util.Base64;

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
import de.dlr.proseo.logging.messages.PlannerMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security configuration for prosEO Planner module
 * 
 * @author Ernst Melchinger
 */
@Configuration
@EnableWebSecurity
public class ProductionPlannerSecurityConfig {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ProductionPlannerSecurityConfig.class);
	
	/**
	 * Parse an HTTP authentication header into username and password
	 *
	 * @param authHeader the authentication header to parse
	 * @return a string array containing the username and the password
	 * @throws IllegalArgumentException if the authentication header cannot be
	 *                                  parsed
	 */
	public String[] parseAuthenticationHeader(String authHeader) throws IllegalArgumentException {
		if (logger.isTraceEnabled())
			logger.trace(">>> parseAuthenticationHeader({})", authHeader);

		if (null == authHeader) {
			String message = logger.log(PlannerMessage.AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] authParts = authHeader.split(" ");
		if (2 != authParts.length || !"Basic".equals(authParts[0])) {
			String message = logger.log(PlannerMessage.AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		// The following is guaranteed to work as per BasicAuth specification (but split
		// limited, because password may contain ':')
		String[] userPassword = (new String(Base64.getDecoder().decode(authParts[1]))).split(":", 2);
		return userPassword;
	}

	/**
	 * Set the Ingestor security options
	 * 
	 * @param http the HTTP security object
	 */
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.httpBasic(it -> {})
		.authorizeHttpRequests(requests -> requests
				.antMatchers("/**/actuator/health").permitAll()
				.antMatchers(HttpMethod.GET, "/**/orders").hasAnyRole(UserRole.ORDER_READER.toString())
				.antMatchers("/**/orders/approve").hasAnyRole(UserRole.ORDER_APPROVER.toString())
				.antMatchers(
						"/**/orders/plan", "/**/orders/release",
						"/**/orders/reset", "/**/orders/cancel",
						"/**/orders/retry", "/**/orders/suspend")
				.hasAnyRole(UserRole.ORDER_PLANNER.toString())
				.antMatchers("/**/orders").hasAnyRole(UserRole.ORDER_MGR.toString())
				.antMatchers(HttpMethod.GET, "/**/jobs", "/**/jobsteps").hasAnyRole(UserRole.ORDER_READER.toString())
				.antMatchers("/**/jobs", "/**/jobsteps").hasAnyRole(UserRole.ORDER_PLANNER.toString())
				.antMatchers("/**/processingfacilities/synchronize")
				.hasAnyRole(UserRole.FACILITY_MGR.toString(), UserRole.ORDER_PLANNER.toString())
				.antMatchers(HttpMethod.GET, "/**/processingfacilities").hasAnyRole(UserRole.FACILITY_READER.toString())
				.antMatchers("/**/processingfacilities/*/finish/*").hasAnyRole(UserRole.JOBSTEP_PROCESSOR.toString())
				.antMatchers("/**/product/*").hasAnyRole(UserRole.PRODUCT_INGESTOR.toString(), UserRole.JOBSTEP_PROCESSOR.toString())
				.antMatchers("/**/semaphore/*").hasAnyRole(UserRole.PRODUCT_INGESTOR.toString(), UserRole.JOBSTEP_PROCESSOR.toString())
				.anyRequest().hasAnyRole(UserRole.ORDER_MGR.toString()))
				.csrf((csrf) -> csrf.disable()); // Required for POST requests (or configure CSRF)
		return http.build();
	}

	/**
	 * Provides the default password encoder for prosEO (BCrypt)
	 * 
	 * @return a BCryptPasswordEncoder
	 */
	@Bean
    PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder();
	}

	/**
	 * Provides the default user details service for prosEO (based on the standard data model for users and groups)
	 * 
	 * @return a JdbcDaoImpl object
	 */
	@Bean
	UserDetailsService userDetailsService() {
		logger.log(GeneralMessage.INITIALIZING_USER_DETAILS_SERVICE, dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);
		
		return jdbcDaoImpl;
	}
}
