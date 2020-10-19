/**
 * ProductionPlannerSecurityConfig.java
 * 
 * © 2019 Prophos Informatik GmbH
 */
package de.dlr.proseo.planner;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import de.dlr.proseo.model.enums.UserRole;

/**
 * Security configuration for prosEO Planner module
 * 
 * @author Ernst Melchinger
 */
@Configuration
@EnableWebSecurity
public class ProductionPlannerSecurityConfig extends WebSecurityConfigurerAdapter {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductionPlannerSecurityConfig.class);
	
	/**
	 * Set the Ingestor security options
	 * 
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic()
				.and()
			.authorizeRequests()
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
				.antMatchers("/**/processingfacilities/*/finish").hasAnyRole(UserRole.JOBSTEP_PROCESSOR.toString())
				.antMatchers("/**/product").hasAnyRole(UserRole.PRODUCT_INGESTOR.toString(), UserRole.JOBSTEP_PROCESSOR.toString())
				.anyRequest().hasAnyRole(UserRole.ORDER_MGR.toString())
			.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the ProductClassManager from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder) throws Exception {
		logger.info("Initializing authentication from user details service ");

		builder.userDetailsService(userDetailsService());
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
		logger.info("Initializing user details service from datasource " + dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);
		
		return jdbcDaoImpl;
	}
}
