/**
 * IngestorSecurityConfig.java
 *
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor;

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
import de.dlr.proseo.logging.messages.IngestorMessage;
import de.dlr.proseo.model.enums.UserRole;

/**
 * Security configuration for prosEO Ingestor module
 *
 * @author Dr. Thomas Bassler
 */
@Configuration
@EnableWebSecurity
public class IngestorSecurityConfig {

	/** Datasource as configured in the application properties */
	@Autowired
	private DataSource dataSource;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(IngestorSecurityConfig.class);

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
			String message = logger.log(IngestorMessage.AUTH_MISSING_OR_INVALID, authHeader);
			throw new IllegalArgumentException(message);
		}
		String[] authParts = authHeader.split(" ");
		if (2 != authParts.length || !"Basic".equals(authParts[0])) {
			String message = logger.log(IngestorMessage.AUTH_MISSING_OR_INVALID, authHeader);
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
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		String base = "/proseo/ingestor/v0.1";
        http.httpBasic(it -> {})
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, base + "/products", base + "/products/**",
                                base + "/ingest/**")
                        .hasAnyRole(UserRole.PRODUCT_READER.toString(), UserRole.PRODUCT_READER_RESTRICTED.toString(),
                                UserRole.PRODUCT_READER_ALL.toString())
                        .requestMatchers(HttpMethod.POST, base + "/ingest/**")
                        .hasAnyRole(UserRole.PRODUCT_INGESTOR.toString())
                        .anyRequest()
                        .hasAnyRole(UserRole.PRODUCT_MGR.toString()))
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
	 * Provides the default user details service for prosEO (based on the standard
	 * data model for users and groups)
	 *
	 * @return a JdbcDaoImpl object
	 */
	@Bean
	UserDetailsService userDetailsService() {
		logger.log(IngestorMessage.INITIALIZE_USER_INFO, dataSource);

		JdbcDaoImpl jdbcDaoImpl = new JdbcDaoImpl();
		jdbcDaoImpl.setDataSource(dataSource);
		jdbcDaoImpl.setEnableGroups(true);

		return jdbcDaoImpl;
	}
}
