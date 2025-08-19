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
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

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
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
    
	/**
	 * Configures the HTTP security settings, including the authentication filter, URL permissions, login and logout pages, and CSRF
	 * protection.
	 *
	 * @param http the HttpSecurity object to be configured
	 * @throws Exception if an error occurs during configuration
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		// Create an instance of the custom authentication filter
		SpringAuthenticationFilter authenticationFilter = new SpringAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(authenticationManagerBean());
		authenticationFilter.setFilterProcessesUrl("/customlogin");

		// Configure HTTP security
		http
            .securityContext(context -> context.securityContextRepository(securityContextRepository()))
            .requestCache(RequestCacheConfigurer::disable)

		    .addFilter(authenticationFilter)
			.authenticationProvider(authenticationProvider)
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
					.permitAll())
			.csrf(csrf -> csrf.disable())
			.headers().httpStrictTransportSecurity().disable()
			;

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

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}