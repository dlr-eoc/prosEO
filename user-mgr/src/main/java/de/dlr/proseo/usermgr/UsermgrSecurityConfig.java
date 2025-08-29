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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.UserMgrMessage;
import de.dlr.proseo.model.enums.UserRole;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.usermgr.rest.UserManager;
import de.dlr.proseo.usermgr.rest.model.RestUser;

/**
 * Security configuration for prosEO UserManager module
 *
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class UsermgrSecurityConfig {

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
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		String base ="/proseo/user-mgr/v0.1";
		http.httpBasic(it -> {})
			.authorizeHttpRequests(requests -> requests
			.requestMatchers("/actuator/health")
			.permitAll()
			.requestMatchers(base + "/login/**")
			.authenticated()
			.requestMatchers(HttpMethod.GET, base + "/users/**")
			.authenticated() // Any user may change their own password
			.requestMatchers(HttpMethod.PATCH, base + "/users/**")
			.authenticated() // Any user may change their own password
			.anyRequest()
			.hasAnyRole(UserRole.ROOT.toString(), UserRole.USERMGR.toString()));
//			.csrf((csrf) -> csrf.disable()); // Required for POST requests (or configure CSRF)
		return http.build();
	}

	/**
	 * Provides the default password encoder for prosEO (BCrypt)
	 *
	 * @return a BCryptPasswordEncoder
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
	}

	/**
	 * Provides the default user details service for prosEO (based on the standard
	 * data model for users and groups)
	 *
	 * @return a JdbcDaoImpl object
	 */
	@Bean
	UserDetailsService userDetailsService() {
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
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);


			for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
				try {
					restUser = transactionTemplate.execute((status) -> {
						return userManager.createUser(transactionalRestUser);
					});
				} catch (CannotAcquireLockException e1) {
					if (logger.isDebugEnabled())
						logger.debug("... database concurrency issue detected: ", e1);

					if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
						ProseoUtil.dbWait();
					} else {
						if (logger.isDebugEnabled())
							logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
						throw e;
					}
				} catch (Exception e1) {
					logger.log(UserMgrMessage.CREATE_BOOTSTRAP_FAILED, e1);
				}
			}

		}

		return jdbcDaoImpl;
	}

}