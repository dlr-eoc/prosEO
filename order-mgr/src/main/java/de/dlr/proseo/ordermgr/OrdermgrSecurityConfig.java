/**
 * OrdermgrSecurityConfig.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr;

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
 * Security configuration for prosEO OrderManager module
 * 
 * @author Ranjitha Vignesh
 */
@Configuration
@EnableWebSecurity
public class OrdermgrSecurityConfig extends WebSecurityConfigurerAdapter {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(OrdermgrSecurityConfig.class);
	
	/**
	 * Set the Ordermgr security options
	 * 
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.httpBasic()
				.and()
			.authorizeRequests()
				.antMatchers(HttpMethod.GET, "/**/missions").permitAll()
				.antMatchers(HttpMethod.POST, "/**/missions").hasAnyRole(UserRole.ROOT.toString())
				.antMatchers(HttpMethod.DELETE, "/**/missions").hasAnyRole(UserRole.ROOT.toString())
				.antMatchers("/**/missions").hasAnyRole(UserRole.MISSION_MGR.toString())

				.antMatchers(HttpMethod.GET, "/**/orders").hasAnyRole(UserRole.ORDER_READER.toString())
				.antMatchers(HttpMethod.PATCH, "/**/orders/*").hasAnyRole(
						UserRole.ORDER_MGR.toString(),
						UserRole.ORDER_APPROVER.toString(),
						UserRole.ORDER_PLANNER.toString())
				.antMatchers(HttpMethod.DELETE, "/**/orders/*").hasAnyRole(UserRole.ORDER_MGR.toString())
				.antMatchers("/**/orders").hasAnyRole(UserRole.ORDER_MGR.toString())

				.antMatchers(HttpMethod.GET, "/**/orbits").hasAnyRole(UserRole.MISSION_READER.toString())
				.antMatchers("/**/orbits").hasAnyRole(UserRole.MISSION_MGR.toString())
				.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the Ordermgr from the prosEO database
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
