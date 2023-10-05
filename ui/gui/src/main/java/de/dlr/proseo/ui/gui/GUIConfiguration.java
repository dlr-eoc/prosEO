/**
 * GUIConfiguration.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Configuration properties and beans related to the GUI (Graphical User Interface) of an application, used to configure various
 * aspects of the GUI, such as the base URIs for different services and components, timeout settings, and localization.
 *
 * @author David Mazo
 */
@Configuration
@ConfigurationProperties(prefix = "proseo")
@EntityScan(basePackages = "de.dlr.proseo.model")
public class GUIConfiguration implements WebMvcConfigurer {

	/** The base URI for the prosEO Processor Manager */
	@Value("${proseo.processorManager.url}")
	private String processorManager;

	/** The base URI for the prosEO Order Manager */
	@Value("${proseo.orderManager.url}")
	private String orderManager;

	/** The base URI for the prosEO Planner */
	@Value("${proseo.productionPlanner.url}")
	private String productionPlanner;

	/** The timeout in ms used for long order actions (asynchronous requests) */
	@Value("${proseo.gui.timeout}")
	private Long timeout;

	/**
	 * Gets the timeout for asynchronous requests in milliseconds.
	 *
	 * @return the timeout in milliseconds
	 */
	public Long getTimeout() {
		return timeout;
	}

	/**
	 * Gets the Production Planner base URI.
	 *
	 * @return the Production Planner URI
	 */
	public String getProductionPlanner() {
		return productionPlanner;
	}

	/**
	 * Gets the Order Manager base URI.
	 *
	 * @return the Order Manager URI
	 */
	public String getOrderManager() {
		return orderManager;
	}

	/**
	 * Gets the Processor Manager base URI.
	 *
	 * @return the Processor Manager URI
	 */
	public String getProcessorManager() {
		return processorManager;
	}

	/**
	 * Creates a locale resolver bean for handling localization.
	 *
	 * @return the locale resolver bean
	 */
	@Bean
	public LocaleResolver localeResolver() {
		return new CookieLocaleResolver();
	}

	/**
	 * Creates a locale change interceptor bean for handling language change.
	 *
	 * @return the locale change interceptor bean
	 */
	@Bean
	public LocaleChangeInterceptor localChangeInterceptor() {
		LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang"); // The request parameter name for language change
		return lci;
	}

	/**
	 * Adds the locale change interceptor to the interceptor registry.
	 *
	 * @param registry the interceptor registry
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localChangeInterceptor());
	}

}