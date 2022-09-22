/**
 * IngestorSecurityConfig.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

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

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security configuration for prosEO Ingestor module
 * 
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class IngestorSecurityConfig extends WebSecurityConfigurerAdapter {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;
	
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestorSecurityConfig.class);
		
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
				.antMatchers("/**/actuator/health").permitAll()
				.antMatchers(HttpMethod.GET, "/**/products", "/**/products/*", "/**/products/*/*", "/**/products/*/*/*", "/**/ingest/*/*")
					.hasAnyRole(
						UserRole.PRODUCT_READER.toString(),
						UserRole.PRODUCT_READER_RESTRICTED.toString(),
						UserRole.PRODUCT_READER_ALL.toString())
				.antMatchers(HttpMethod.POST, "/**/ingest/*").hasAnyRole(UserRole.PRODUCT_INGESTOR.toString())
				.antMatchers(HttpMethod.POST, "/**/ingest/*/*").hasAnyRole(UserRole.PRODUCT_GENERATOR.toString())
				.anyRequest().hasAnyRole(UserRole.PRODUCT_MGR.toString())
				.and()
			.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the Ingestor from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder) throws Exception {
		logger.log(IngestorMessage.INITIALIZE_AUTHENTICATION);

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
		logger.log(IngestorMessage.INITIALIZE_USER_INFO, dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);
		
		return jdbcDaoImpl;
	}
}
