package de.dlr.proseo.ui.gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private GUIAuthenticationProvider authenticationProvider;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {	   
		SpringAuthenticationFilter authenticationFilter = new SpringAuthenticationFilter();
		authenticationFilter.setAuthenticationManager(authenticationManagerBean());
		authenticationFilter.setFilterProcessesUrl("/customlogin");
		http.addFilter(authenticationFilter)
			.authorizeRequests()
				.antMatchers("/resources/**").permitAll()
				.antMatchers("/background.jpg").permitAll()
				.anyRequest().authenticated()
			.and()
				.formLogin()
					.loginPage("/customlogin")
					.failureUrl("/customlogin?error").permitAll()
			.and()
				.logout()
					.logoutUrl("/logout")
					.logoutSuccessUrl("/customlogin?logout").permitAll().and().csrf().disable();
	}

	@Override
	protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(authenticationProvider);
		super.configure(auth);
	}
}
