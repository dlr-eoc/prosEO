package de.dlr.proseo.monitor;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security configuration for prosEO Planner module
 * 
 * @author Ernst Melchinger
 */
@Configuration
@EnableWebSecurity
public class MonitorSecurityConfig {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(MonitorSecurityConfig.class);
	
	/**
	 * Set the Ingestor security options
	 * 
	 * @param http the HTTP security object
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.httpBasic(it -> {})
        .authorizeHttpRequests(requests -> requests
			.anyRequest().authenticated())
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
