/**
 * ProductUtil.java
 * 
 * (c) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ingestor.rest.model;

/**
 * Utility methods for products, e. g. for conversion between prosEO model and REST model
 * 
 * @author Dr. Thomas Bassler
 */
public class ProductUtil {

	/**
	 * Convert a prosEO model product into a REST product
	 * 
	 * @param modelProduct the prosEO model product
	 * @return an equivalent REST product or null, if no model product was given
	 */
	public static Product toRestProduct(de.dlr.proseo.model.Product modelProduct) {
		if (null == modelProduct)
			return null;
		
		Product restProduct = new Product();
		
		restProduct.setId(modelProduct.getId());
		restProduct.setVersion(1L + modelProduct.getVersion());
		//restProduct.setMission(productToModify.getProductClass().getMission().getCode());
		restProduct.setProductClass(modelProduct.getProductClass().getProductType());
		restProduct.setMode(modelProduct.getMode());
		restProduct.setSensingStartTime(de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getSensingStartTime()));
		restProduct.setSensingStopTime(de.dlr.proseo.model.Orbit.orbitTimeFormatter.format(modelProduct.getSensingStopTime()));
		for (de.dlr.proseo.model.Product componentProduct: modelProduct.getComponentProducts()) {
			restProduct.getComponentProductIds().add(componentProduct.getId());
		}
		if (null != modelProduct.getEnclosingProduct()) {
			restProduct.setEnclosingProductId(modelProduct.getEnclosingProduct().getId());
		}
		if (null != modelProduct.getOrbit()) {
			Orbit restOrbit = new Orbit();
			de.dlr.proseo.model.Orbit modelOrbit = modelProduct.getOrbit();
			restOrbit.setOrbitNumber(Long.valueOf(modelOrbit.getOrbitNumber()));
			restOrbit.setSpacecraftCode(modelOrbit.getSpacecraft().getCode());
			restProduct.setOrbit(restOrbit);
		}
		for (de.dlr.proseo.model.ProductFile modelFile: modelProduct.getProductFile()) {
			ProductFile restFile = new ProductFile();
			restFile.setId(modelFile.getId());
			restFile.setVersion(Long.valueOf(modelFile.getVersion()));
			restFile.setProcessingFacilityName(modelFile.getProcessingFacility().getName());
			restFile.setProductFileName(modelFile.getProductFileName());
			restFile.setFilePath(modelFile.getFilePath());
			restFile.setStorageType(modelFile.getStorageType().toString());
			for (String modelAuxFileName: modelFile.getAuxFileNames()) {
				restFile.getAuxFileNames().add(modelAuxFileName);
			}
			restProduct.getProductFile().add(restFile);
		}
		for (String productParameterKey: modelProduct.getParameters().keySet()) {
			de.dlr.proseo.ingestor.rest.model.Parameter restParameter = new de.dlr.proseo.ingestor.rest.model.Parameter(
					productParameterKey,
					modelProduct.getParameters().get(productParameterKey).getParameterType().toString(),
					modelProduct.getParameters().get(productParameterKey).getParameterValue().toString());
			restProduct.getParameters().add(restParameter);
		}
		
		return restProduct;
	}
	
	public static de.dlr.proseo.model.Product toModelProduct(Product restProduct) {
		// TODO
		throw new UnsupportedOperationException();
	}
}
