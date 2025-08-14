/**
 * OrdermgrSecurityConfig.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

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
 * Security configuration for prosEO OrderManager module
 *
 * @author Ranjitha Vignesh
 */
@Configuration
@EnableWebSecurity
public class OrdermgrSecurityConfig {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrdermgrSecurityConfig.class);

	/**
	 * Set the order manager's security options
	 *
	 * @param http the HTTP security object
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		String base ="/proseo/order-mgr/v0.1";
		http.httpBasic(it -> {})
		.authorizeHttpRequests(requests -> requests
			.requestMatchers("/actuator/health")
			.permitAll()
			.requestMatchers(HttpMethod.GET, base + "/missions")
			.permitAll()
			.requestMatchers(HttpMethod.POST, base + "/missions")
			.hasAnyRole(UserRole.ROOT.toString())
			.requestMatchers(HttpMethod.DELETE, base + "/missions")
			.hasAnyRole(UserRole.ROOT.toString())
			.requestMatchers(base + "/missions")
			.hasAnyRole(UserRole.MISSION_MGR.toString())

			.requestMatchers(HttpMethod.GET, base + "/orders")
			.hasAnyRole(UserRole.ORDER_READER.toString())
			.requestMatchers(HttpMethod.PATCH, base + "/orders/*")
			.hasAnyRole(UserRole.ORDER_MGR.toString(), UserRole.ORDER_APPROVER.toString(), UserRole.ORDER_PLANNER.toString())
			.requestMatchers(HttpMethod.DELETE, base + "/orders/*")
			.hasAnyRole(UserRole.ORDER_MGR.toString())
			.requestMatchers(base + "/orders")
			.hasAnyRole(UserRole.ORDER_MGR.toString())

			.requestMatchers(HttpMethod.GET, base + "/orbits")
			.hasAnyRole(UserRole.MISSION_READER.toString())
			.requestMatchers(base + "/orbits")
			.hasAnyRole(UserRole.MISSION_MGR.toString()))
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