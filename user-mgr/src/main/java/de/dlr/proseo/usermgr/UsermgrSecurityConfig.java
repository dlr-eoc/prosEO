/**
 * UsermgrSecurityConfig.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.EhCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for prosEO UserManager module
 * 
 * @author Ranjitha Vignesh
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class UsermgrSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	DataSource dataSource;

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
//		.antMatchers("/login").authenticated()
//		.anyRequest().hasAnyRole("ROOT", "USERMGR")
		.anyRequest().authenticated()
		.and()
		.csrf().disable(); // Required for POST requests (or configure CSRF)
	}

	/**
	 * Initialize the users, passwords and roles for the User Manager from the prosEO database
	 * 
	 * @param builder to manage authentications
	 * @param dataSource the data source configured for the User Manager
	 * @throws Exception if anything goes wrong with JDBC authentication
	 */
	@Autowired
	public void initialize(AuthenticationManagerBuilder builder, DataSource dataSource) throws Exception {
		logger.info("Initializing authentication from datasource " + dataSource);

		builder.jdbcAuthentication()
		.dataSource(dataSource);

	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
	}

	@Bean 
	public JdbcMutableAclService aclService() { 
		JdbcMutableAclService aclService = new JdbcMutableAclService(
				dataSource, lookupStrategy(), aclCache());
		aclService.setClassIdentityQuery("select currval(pg_get_serial_sequence('acl_class', 'id'))");
		aclService.setSidIdentityQuery("select currval(pg_get_serial_sequence('acl_sid', 'id'))");
		return aclService;
	}
	@Bean
	public AclAuthorizationStrategy aclAuthorizationStrategy() {
		return new AclAuthorizationStrategyImpl(
				new SimpleGrantedAuthority("ROLE_ROOT"));
	}

	@Bean
	public PermissionGrantingStrategy permissionGrantingStrategy() {
		return new DefaultPermissionGrantingStrategy(
				new ConsoleAuditLogger());
	}

	@Bean
	public EhCacheBasedAclCache aclCache() {
		return new EhCacheBasedAclCache(
				aclEhCacheFactoryBean().getObject(), 
				permissionGrantingStrategy(), 
				aclAuthorizationStrategy()
				);
	}

	@Bean
	public EhCacheFactoryBean aclEhCacheFactoryBean() {
		EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
		ehCacheFactoryBean.setCacheManager(aclCacheManager().getObject());
		ehCacheFactoryBean.setCacheName("aclCache");
		return ehCacheFactoryBean;
	}

	@Bean
	public EhCacheManagerFactoryBean aclCacheManager() {
		return new EhCacheManagerFactoryBean();
	}

	@Bean
	public LookupStrategy lookupStrategy() { 
		return new BasicLookupStrategy(
				dataSource, 
				aclCache(), 
				aclAuthorizationStrategy(), 
				new ConsoleAuditLogger()
				); 
	}
}
