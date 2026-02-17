package de.dlr.proseo.ordermgr.rest.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.ConfiguredProcessor;
import de.dlr.proseo.model.NotificationEndpoint;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.OrderTemplate;
import de.dlr.proseo.model.enums.OrderSlicingType;
import de.dlr.proseo.model.rest.model.RestClassOutputParameter;
import de.dlr.proseo.model.rest.model.RestInputFilter;
import de.dlr.proseo.model.rest.model.RestNotificationEndpoint;
import de.dlr.proseo.model.rest.model.RestOrderTemplate;
import de.dlr.proseo.model.rest.model.RestParameter;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.ProductClass;

public class OrderTemplateUtil {
	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderTemplateUtil.class);
	
	/**
	 * Convert a prosEO model OrderTemplate into a REST OrderTemplate
	 * 
	 * @param orderTemplate the prosEO model OrderTemplate
	 * @return an equivalent REST OrderTemplate or null, if no model OrderTemplate was given
	 */

	public static RestOrderTemplate toRestOrderTemplate(OrderTemplate orderTemplate) {
		if (logger.isTraceEnabled()) logger.trace(">>> toRestOrderTemplate({})", (null == orderTemplate ? "MISSING" : orderTemplate.getId()));
		
		if (null == orderTemplate)
			return null;
		
		RestOrderTemplate restOrderTemplate = new RestOrderTemplate();
		Comparator<String> c = Comparator.comparing((String x) -> x);
		
		restOrderTemplate.setId(orderTemplate.getId());
		restOrderTemplate.setVersion(Long.valueOf(orderTemplate.getVersion()));
		
		if (null != orderTemplate.getMission()) {
			restOrderTemplate.setMissionCode(orderTemplate.getMission().getCode());

		}	
		if (null != orderTemplate.getName()) {
			restOrderTemplate.setName(orderTemplate.getName());
		}	
		if (null != orderTemplate.getPriority()) {
			restOrderTemplate.setPriority(orderTemplate.getPriority());
		}	
		if(null != orderTemplate.getSlicingType()) {
			restOrderTemplate.setSlicingType(orderTemplate.getSlicingType().name());
		}
		if(null != orderTemplate.getSliceDuration()) {
			restOrderTemplate.setSliceDuration(orderTemplate.getSliceDuration().getSeconds());
		}
		restOrderTemplate.setSliceOverlap(orderTemplate.getSliceOverlap().getSeconds());

		if (null != orderTemplate.getInputFilters()) {
			for (ProductClass sourceClass : orderTemplate.getInputFilters().keySet()) {
				RestInputFilter restInputFilter = new RestInputFilter();
				restInputFilter.setProductClass(sourceClass.getProductType());
				Map<String, Parameter> filterConditions = orderTemplate.getInputFilters().get(sourceClass)
						.getFilterConditions();
				for (String paramKey : filterConditions.keySet()) {
					restInputFilter.getFilterConditions()
							.add(new RestParameter(paramKey,
									filterConditions.get(paramKey).getParameterType().toString(),
									filterConditions.get(paramKey).getParameterValue()));
				}
				Collections.sort(restInputFilter.getFilterConditions(), (o1, o2) -> {
					return o1.getKey().compareTo(o2.getKey());
				});
				restOrderTemplate.getInputFilters().add(restInputFilter);
			}
			Collections.sort(restOrderTemplate.getInputFilters(), (o1, o2) -> {
				return o1.getProductClass().compareTo(o2.getProductClass());
			});
		}

		if (null != orderTemplate.getClassOutputParameters()) {
			for (ProductClass targetClass : orderTemplate.getClassOutputParameters().keySet()) {
				RestClassOutputParameter restClassOutputParameter = new RestClassOutputParameter();
				restClassOutputParameter.setProductClass(targetClass.getProductType());
				Map<String, Parameter> outputParameters = orderTemplate.getClassOutputParameters().get(targetClass)
						.getOutputParameters();
				for (String paramKey : outputParameters.keySet()) {
					restClassOutputParameter.getOutputParameters()
							.add(new RestParameter(paramKey,
									outputParameters.get(paramKey).getParameterType().toString(),
									outputParameters.get(paramKey).getParameterValue()));
				}
				Collections.sort(restClassOutputParameter.getOutputParameters(), (o1, o2) -> {
					return o1.getKey().compareTo(o2.getKey());
				});
				restOrderTemplate.getClassOutputParameters().add(restClassOutputParameter);
			}
			Collections.sort(restOrderTemplate.getClassOutputParameters(), (o1, o2) -> {
				return o1.getProductClass().compareTo(o2.getProductClass());
			});
		}
		
		if(null != orderTemplate.getOutputParameters()) {
			
			for (String paramKey: orderTemplate.getOutputParameters().keySet()) {
				restOrderTemplate.getOutputParameters().add(
					new RestParameter(paramKey,
							orderTemplate.getOutputParameters().get(paramKey).getParameterType().toString(),
							orderTemplate.getOutputParameters().get(paramKey).getParameterValue()));
			}
			Collections.sort(restOrderTemplate.getOutputParameters(), (o1, o2) -> {
				return o1.getKey().compareTo(o2.getKey());
			});
		}
		if (null != orderTemplate.getRequestedProductClasses()) {
			
			for (ProductClass productClass : orderTemplate.getRequestedProductClasses()) {
				restOrderTemplate.getRequestedProductClasses().add(productClass.getProductType());
			}
			restOrderTemplate.getRequestedProductClasses().sort(c);
			
		}
		if (null != orderTemplate.getInputProductClasses()) {
			for (ProductClass productClass : orderTemplate.getInputProductClasses()) {
				restOrderTemplate.getInputProductClasses().add(productClass.getProductType());
			}
			restOrderTemplate.getInputProductClasses().sort(c);
		}
		
		if(null != orderTemplate.getOutputFileClass()) {
			restOrderTemplate.setOutputFileClass(orderTemplate.getOutputFileClass());
		}
		
		if(null != orderTemplate.getProcessingMode()) {
			restOrderTemplate.setProcessingMode(orderTemplate.getProcessingMode());
		}
		
		if (null != orderTemplate.getProductRetentionPeriod()) {
			restOrderTemplate.setProductRetentionPeriod(orderTemplate.getProductRetentionPeriod().getSeconds());
		}

		if(null != orderTemplate.getInputDataTimeoutPeriod()) {
			restOrderTemplate.setInputDataTimeoutPeriod(orderTemplate.getInputDataTimeoutPeriod().getSeconds());
		}
		restOrderTemplate.setOnInputDataTimeoutFail(orderTemplate.isOnInputDataTimeoutFail());
		restOrderTemplate.setAutoRelease(orderTemplate.isAutoRelease());
		restOrderTemplate.setAutoClose(orderTemplate.isAutoClose());
		restOrderTemplate.setEnabled(orderTemplate.isEnabled());
		
		if (null != orderTemplate.getRequestedConfiguredProcessors()) {
			for (ConfiguredProcessor toAddProcessor: orderTemplate.getRequestedConfiguredProcessors()) {
				restOrderTemplate.getConfiguredProcessors().add(toAddProcessor.getIdentifier());
			}
			restOrderTemplate.getConfiguredProcessors().sort(c);
		}	
		
		if (null != orderTemplate.getDynamicProcessingParameters()
				& !orderTemplate.getDynamicProcessingParameters().isEmpty()) {
			List<RestParameter> dynamicProcessingParameters = new ArrayList<>();
			for (String key : orderTemplate.getDynamicProcessingParameters().keySet()) {
				Parameter param = orderTemplate.getDynamicProcessingParameters().get(key);
				RestParameter restParam = new RestParameter(key, param.getParameterType().name(),
						param.getStringValue());
				dynamicProcessingParameters.add(restParam);
			}
			Collections.sort(dynamicProcessingParameters, (o1, o2) -> {
				return o1.getKey().compareTo(o2.getKey());
			});
			restOrderTemplate.setDynamicProcessingParameters(dynamicProcessingParameters);
		}
		
		if (null != orderTemplate.getNotificationEndpoint()) {
			NotificationEndpoint notificationEndpoint = orderTemplate.getNotificationEndpoint();
			RestNotificationEndpoint restNotificationEndpoint = new RestNotificationEndpoint();
			restNotificationEndpoint.setPassword(notificationEndpoint.getPassword());
			restNotificationEndpoint.setUri(notificationEndpoint.getUri());
			restNotificationEndpoint.setUsername(notificationEndpoint.getUsername());
			restOrderTemplate.setNotificationEndpoint(restNotificationEndpoint);
		}
			
		return restOrderTemplate;
	}

	/**
	 * Convert a REST order into a prosEO model processingorder (scalar and embedded attributes only, no orbit references)
	 * 
	 * @param restOrderTemplate the REST OrderTemplate
	 * @return a (roughly) equivalent model OrderTemplate
	 * @throws IllegalArgumentException if the REST order violates syntax rules for date, enum or numeric values
	 */
	public static OrderTemplate toModelOrderTemplate(RestOrderTemplate restOrderTemplate) throws IllegalArgumentException {
		
		if (logger.isTraceEnabled()) logger.trace(">>> toModelOrder({})", (null == restOrderTemplate ? "MISSING" : restOrderTemplate.getId()));
		
		OrderTemplate orderTemplate = new OrderTemplate();
		
		if (null != restOrderTemplate.getId() && 0 != restOrderTemplate.getId()) {
			orderTemplate.setId(restOrderTemplate.getId());
			while (orderTemplate.getVersion() < restOrderTemplate.getVersion()) {
				orderTemplate.incrementVersion();
			} 
		}
		if (null != restOrderTemplate.getMissionCode()) {
			orderTemplate.setMission(RepositoryService.getMissionRepository().findByCode(restOrderTemplate.getMissionCode()));
		}		
		orderTemplate.setName(restOrderTemplate.getName());
		orderTemplate.setPriority(restOrderTemplate.getPriority());
		orderTemplate.setProcessingMode(restOrderTemplate.getProcessingMode());

		if (null != restOrderTemplate.getOutputFileClass()) {
			orderTemplate.setOutputFileClass(restOrderTemplate.getOutputFileClass());
		}
		if (null != restOrderTemplate.getSlicingType()) {
			orderTemplate.setSlicingType(OrderSlicingType.valueOf(restOrderTemplate.getSlicingType()));	

		}
		if (null != restOrderTemplate.getSliceDuration()) {
			orderTemplate.setSliceDuration(Duration.ofSeconds(restOrderTemplate.getSliceDuration()));

		}
		if (null != restOrderTemplate.getSliceOverlap()) {
			orderTemplate.setSliceOverlap(Duration.ofSeconds(restOrderTemplate.getSliceOverlap()));

		}
		
		for (RestParameter restParam: restOrderTemplate.getOutputParameters()) {
			Parameter modelParam = new Parameter();
			modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
			orderTemplate.getOutputParameters().put(restParam.getKey(), modelParam);
		}
		
		if (null != restOrderTemplate.getOutputFileClass()) {
			orderTemplate.setOutputFileClass(restOrderTemplate.getOutputFileClass());
		}
		if (null != restOrderTemplate.getProcessingMode()) {
			orderTemplate.setProcessingMode(restOrderTemplate.getProcessingMode());
		}
		if (null != restOrderTemplate.getProductRetentionPeriod()) {
			orderTemplate.setProductRetentionPeriod(Duration.ofSeconds(restOrderTemplate.getProductRetentionPeriod()));
		}

		if(null != restOrderTemplate.getInputDataTimeoutPeriod()) {
			orderTemplate.setInputDataTimeoutPeriod(Duration.ofSeconds(restOrderTemplate.getInputDataTimeoutPeriod()));
		}
		if (null != restOrderTemplate.getOnInputDataTimeoutFail()) {
			orderTemplate.setOnInputDataTimeoutFail(restOrderTemplate.getOnInputDataTimeoutFail());
		}
		if (null != restOrderTemplate.getAutoRelease()) {
			orderTemplate.setAutoRelease(restOrderTemplate.getAutoRelease());
		}
		if (null != restOrderTemplate.getAutoClose()) {
			orderTemplate.setAutoClose(restOrderTemplate.getAutoClose());
		}
		if (null != restOrderTemplate.getEnabled()) {
			orderTemplate.setEnabled(restOrderTemplate.getEnabled());
		}
		
		if (null != restOrderTemplate.getDynamicProcessingParameters()
				& !restOrderTemplate.getDynamicProcessingParameters().isEmpty()) {
			for (RestParameter restParam : restOrderTemplate.getDynamicProcessingParameters()) {
				Parameter modelParam = new Parameter();
				modelParam.init(ParameterType.valueOf(restParam.getParameterType()), restParam.getParameterValue());
				orderTemplate.getDynamicProcessingParameters().put(restParam.getKey(), modelParam);
			}
		}

		if (null != restOrderTemplate.getNotificationEndpoint()) {
			RestNotificationEndpoint restNotificationEndpoint = restOrderTemplate.getNotificationEndpoint();
			NotificationEndpoint notificationEndpoint = new NotificationEndpoint();
			notificationEndpoint.setPassword(restNotificationEndpoint.getPassword());
			notificationEndpoint.setUri(restNotificationEndpoint.getUri());
			notificationEndpoint.setUsername(restNotificationEndpoint.getUsername());
			orderTemplate.setNotificationEndpoint(notificationEndpoint);
		}
				
		return orderTemplate;
	}
	
}