/**
 * SpringSecurityConfig.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration class for Spring Security. Enables web security and provides customization for authentication and authorization.
 * Extends WebSecurityConfigurerAdapter to override default configurations. Configures the authentication filter, URL permissions,
 * login and logout pages, and CSRF protection. Uses a GUIAuthenticationProvider for authentication.
 *
 */
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

	/** The GUI authentication provider */
	@Autowired
	private GUIAuthenticationProvider authenticationProvider;

	/**
	 * Configures the HTTP security settings, including the authentication filter, URL permissions, login and logout pages, and CSRF
	 * protection.
	 *
	 * @param http the HttpSecurity object to be configured
	 * @throws Exception if an error occurs during configuration
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		
		// Create an instance of the custom authentication filter
		SpringAuthenticationFilter authenticationFilter = new SpringAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(http.getSharedObject(AuthenticationManager.class));
		authenticationFilter.setFilterProcessesUrl("/customlogin");

		// Configure HTTP security
		http.authenticationProvider(authenticationProvider);
		
		http.addFilter(authenticationFilter)
			.authorizeRequests()
			.antMatchers("/resources/**")
			.permitAll()
			.antMatchers("/background.jpg")
			.permitAll()
			.antMatchers("/**/actuator/health")
			.permitAll()
			.anyRequest()
			.authenticated()
			.and()
			.formLogin()
			.loginPage("/customlogin")
			.failureUrl("/customlogin?error")
			.permitAll()
			.and()
			.logout()
			.logoutUrl("/logout")
			.logoutSuccessUrl("/customlogin?logout")
			.permitAll()
			.and()
			.csrf()
			.disable();
		
		return http.build();
	}

}