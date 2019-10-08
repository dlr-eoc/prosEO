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
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for prosEO OrderManager module
 * 
 * @author Ranjitha Vignesh
 */
@Configuration
@EnableWebSecurity
public class OrdermgrSecurityConfig extends WebSecurityConfigurerAdapter {

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
				.anyRequest().authenticated()
				.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the Ordermgr from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @param dataSource the data source configured for the Ordermgr
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder, DataSource dataSource) throws Exception {
		logger.info("Initializing authentication from datasource " + dataSource);

		builder.jdbcAuthentication()
			.dataSource(dataSource);
		
//	    builder.jdbcAuthentication()
//            .dataSource(dataSource).withUser("user").password(passwordEncoder().encode("password")).roles("USER");
		
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
	}

}
