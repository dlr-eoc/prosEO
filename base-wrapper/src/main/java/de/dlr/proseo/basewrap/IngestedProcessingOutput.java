package de.dlr.proseo.basewrap;

public class IngestedProcessingOutput {
	private long product_id;
	private String fsType;
	private String path;
	private long revision;
	private String ingestorHttpResponse;
	/**
	 * @return the product_id
	 */
	public long getProduct_id() {
		return product_id;
	}
	/**
	 * @param product_id the product_id to set
	 */
	public void setProduct_id(long product_id) {
		this.product_id = product_id;
	}
	/**
	 * @return the fsType
	 */
	public String getFsType() {
		return fsType;
	}
	/**
	 * @param fsType the fsType to set
	 */
	public void setFsType(String fsType) {
		this.fsType = fsType;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * @return the revision
	 */
	public long getRevision() {
		return revision;
	}
	/**
	 * @param revision the revision to set
	 */
	public void setRevision(long revision) {
		this.revision = revision;
	}
	/**
	 * @return the ingestorHttpResponse
	 */
	public String getIngestorHttpResponse() {
		return ingestorHttpResponse;
	}
	/**
	 * @param ingestorHttpResponse the ingestorHttpResponse to set
	 */
	public void setIngestorHttpResponse(String ingestorHttpResponse) {
		this.ingestorHttpResponse = ingestorHttpResponse;
	}

	


}
