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

@Configuration
@ConfigurationProperties(prefix="proseo")
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
	 * Gets the timeout for asynchronous requests in ms
	 * 
	 * @return the timeout in ms
	 */
	public Long getTimeout() {
		return timeout;
	}

	/**
	 * Gets the Production Planner base URI
	 * 
	 * @return the Production Planner URI
	 */
	public String getProductionPlanner() {
		return productionPlanner;
	}

	/**
	 * Gets the Order Manager base URI
	 * 
	 * @return the Order Manager URI
	 */
	public String getOrderManager() {
		return orderManager;
	}

	/**
	 * Gets the Processor Manager base URI
	 * 
	 * @return the Processor Manager URI
	 */
	public String getProcessorManager() {
		return processorManager;
	}
	
	@Bean
	public LocaleResolver localeResolver() {
		return new CookieLocaleResolver();
	}
	
	@Bean
	public LocaleChangeInterceptor localChangeInterceptor() {
		LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang");
		return lci;
	}
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localChangeInterceptor());
	}


}
