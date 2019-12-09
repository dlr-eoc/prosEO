package de.dlr.proseo.ui.gui;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
	 @Override
	    protected void configure(HttpSecurity http) throws Exception {
	        http
	            .authorizeRequests()
	            .antMatchers("/resources/**").permitAll()
	            .antMatchers("/background.jpg").permitAll()
	                .anyRequest().authenticated()
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
	    		.csrf().disable();
	    }
}
