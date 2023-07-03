/**
 * UsermgrSecurityConfig.java
 *
 * (c) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr;

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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UserMgrMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.usermgr.rest.UserManager;
import de.dlr.proseo.usermgr.rest.model.RestUser;

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
	private static ProseoLogger logger = new ProseoLogger(UsermgrSecurityConfig.class);

	/**
	 * Set the User Manager security options
	 *
	 * @param http the HTTP security object
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.httpBasic()
			.and()
			.authorizeRequests()
			.antMatchers("/**/actuator/health")
			.permitAll()
			.antMatchers("/**/login")
			.authenticated()
			.antMatchers(HttpMethod.GET, "/**/users/*")
			.authenticated() // Any user may change their own password
			.antMatchers(HttpMethod.PATCH, "/**/users/*")
			.authenticated() // Any user may change their own password
			.anyRequest()
			.hasAnyRole(UserRole.ROOT.toString(), UserRole.USERMGR.toString())
			.and()
			.csrf()
			.disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the User Manager from the
	 * prosEO database
	 *
	 * @param builder to manage authentications
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder) throws Exception {
		logger.log(GeneralMessage.INITIALIZING_AUTHENTICATION);

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
	 * Provides the default user details service for prosEO (based on the standard
	 * data model for users and groups)
	 *
	 * @return a JdbcDaoImpl object
	 */
	@Override
	@Bean
	public UserDetailsService userDetailsService() {
		logger.log(GeneralMessage.INITIALIZING_USER_DETAILS_SERVICE, dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);

		// Make sure one initial user exists
		try {
			jdbcDaoImpl.loadUserByUsername(config.getDefaultUserName());
		} catch (UsernameNotFoundException e) {
			logger.log(UserMgrMessage.CREATE_BOOTSTRAP_USER);
			RestUser restUser = new RestUser();
			restUser.setUsername(config.getDefaultUserName());
			restUser.setPassword(passwordEncoder().encode(config.getDefaultUserPassword()));
			restUser.setEnabled(true);
			restUser.getAuthorities().add(UserRole.ROOT.asRoleString());
			restUser.getAuthorities().add(UserRole.CLI_USER.asRoleString());
			final RestUser transactionalRestUser = restUser;

			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			try {
				restUser = transactionTemplate.execute((status) -> {
					return userManager.createUser(transactionalRestUser);
				});
			} catch (Exception e1) {
				logger.log(UserMgrMessage.CREATE_BOOTSTRAP_FAILED, e1);
			}
		}

		return jdbcDaoImpl;
	}

}