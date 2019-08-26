/*
 * ProductionPlanner.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */

package de.dlr.proseo.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dlr.proseo.planner.kubernetes.KubeConfig;

/*
 * prosEO Ingestor application
 * 
 * @author Dr. Thomas Bassler
 * 
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"de.dlr.proseo"})
@Transactional
@EnableJpaRepositories("de.dlr.proseo.model")
public class ProductionPlanner {
	static Map<String, KubeConfig> kubeConfigs = new HashMap<>();

	public static KubeConfig getKubeConfig(String name) {
		if (name == null && kubeConfigs.size() == 1) {
			return (KubeConfig) kubeConfigs.values().toArray()[0];
		}
		return kubeConfigs.get(name.toLowerCase());
	}

	public static Collection<KubeConfig> getKubeConfigs() {
		return (Collection<KubeConfig>) kubeConfigs.values();
	}
	
	public static void main(String[] args) throws Exception {

		ProductionPlanner.addKubeConfig("Lerchenhof", "Testumgebung auf dem Lerchenhof", "http://192.168.20.159:8080");
		
		SpringApplication.run(ProductionPlanner.class, args);
	}

	public static KubeConfig addKubeConfig(String name, String desc, String url) {
		if (getKubeConfig(name) == null) {
			KubeConfig kubeConfig = new KubeConfig(name, desc, url);
			if (kubeConfig != null && kubeConfig.connect()) {
				kubeConfigs.put(name.toLowerCase(), kubeConfig);
				return kubeConfig;
			}
		}
		return null;
	}

}
