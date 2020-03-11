/**
 * UsermgrSecurityConfig.java
 * 
 * (c) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr;

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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.usermgr.rest.model.RestUser;
import de.dlr.proseo.usermgr.rest.UserManager;

/**
 * Security configuration for prosEO UserManager module
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class UsermgrSecurityConfig extends WebSecurityConfigurerAdapter {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;
	
	/** The User Manager configuration */
	@Autowired
	private UsermgrConfiguration config;

	/** The user manager */
	@Autowired
	private UserManager userManager;

	/** Transaction manager for transaction control */
	@Autowired
	private PlatformTransactionManager txManager;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(UsermgrSecurityConfig.class);

	/**
	 * Set the User Manager security options
	 * 
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
		.httpBasic()
		.and()
		.authorizeRequests()
			.antMatchers("/**/login").authenticated()
			.anyRequest().hasAnyRole("ROOT", "USERMGR")
		.and()
		.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the User Manager from the prosEO database
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
		return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
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
		
		// Make sure one initial user exists
		try {
			jdbcDaoImpl.loadUserByUsername("sysadm");
		} catch (UsernameNotFoundException e) {
			logger.info("Creating bootstrap user");
			RestUser restUser = new RestUser();
			restUser.setUsername(config.getDefaultUserName());
			restUser.setPassword(passwordEncoder().encode(config.getDefaultUserPassword()));
			restUser.setEnabled(true);
			restUser.getAuthorities().add("ROLE_ROOT");
			final RestUser transactionalRestUser = restUser;
			
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			try {
				restUser = transactionTemplate.execute((status) -> {
					return userManager.createUser(transactionalRestUser);
				});
			} catch (Exception e1) {
				logger.error("Creation of bootstrap user failed (cause: " + e1.getMessage() + ")", e1);
			}
		}
		
		return jdbcDaoImpl;
	}
}
