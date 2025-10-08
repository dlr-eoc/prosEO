/**
 * SpringSecurityConfig.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
      AuthenticationManagerBuilder authenticationManagerBuilder =
          http.getSharedObject(AuthenticationManagerBuilder.class);
      authenticationManagerBuilder.authenticationProvider(authenticationProvider);
      return authenticationManagerBuilder.build();
    }

	/**
	 * Configures the HTTP security settings, including the authentication filter, URL permissions, login and logout pages, and CSRF
	 * protection.
	 *
	 * @param http the HttpSecurity object to be configured
	 * @throws Exception if an error occurs during configuration
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// Configure HTTP security
		http // .addFilter(authenticationFilter)
			// .authenticationProvider(authenticationProvider)
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/static", "/fragments", "/background.jpg", "/customlogin", "/actuator/health")
				.permitAll()
				.anyRequest()
				.authenticated())

            .formLogin((form) -> form
                .loginPage("/customlogin")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/customlogin?error=true")
                .permitAll())
			.logout(logout -> logout
					.logoutUrl("/logout")
					.logoutSuccessUrl("/customlogin?logout")
					.permitAll());

		return http.build();
	}

	/**
	 * Provides the authentication manager, which manages authentication attempts within the application.
	 *
	 * @return An instance of AuthenticationManager configured with the GUIAuthenticationProvider.
	 * @throws Exception if an error occurs during the instantiation of AuthenticationManager.
	 */

	@Bean
	AuthenticationManager authenticationManagerBean() throws Exception {
		return new ProviderManager(Collections.singletonList(authenticationProvider));
	}

// 	@Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
//        return configuration.getAuthenticationManager();
//    }
 }
