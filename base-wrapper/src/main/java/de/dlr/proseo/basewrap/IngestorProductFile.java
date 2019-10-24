package de.dlr.proseo.basewrap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class IngestorProductFile implements Serializable
{

    /**
	 * 
	 */
	private static final long serialVersionUID = -8195937933630173347L;
	/**
     * The persistent id of this object
     * 
     */
    protected Long id;
    /**
     * A version identifier to track updates to the object (especially to detect concurrent update attempts)
     * 
     */
    protected Long version;
    /**
     * Id value of the product this product file belongs to
     * 
     */
    protected Long productId;
    /**
     * The name of the processing facility, in which this product file is stored
     * 
     */
    protected String processingFacilityName;
    /**
     * The name of the product data file
     * 
     */
    protected String productFileName;
    /**
     * The auxiliary files for this product file
     * 
     */
    protected List<String> auxFileNames = new ArrayList<String>();
    /**
     * The path to the product files (POSIX file path, S3 bucket etc.)
     * 
     */
    protected String filePath;
    /**
     * The string representation of the type of the storage location (e. g. "S3", "POSIX")
     * 
     */
    protected String storageType;

    /**
     * Creates a new ProductFile.
     * 
     */
    public IngestorProductFile() {
        super();
    }

    /**
     * Creates a new ProductFile.
     * 
     */
    public IngestorProductFile(Long id, Long version, Long productId, String processingFacilityName, String productFileName, List<String> auxFileNames, String filePath, String storageType) {
        super();
        this.id = id;
        this.version = version;
        this.productId = productId;
        this.processingFacilityName = processingFacilityName;
        this.productFileName = productFileName;
        this.auxFileNames = auxFileNames;
        this.filePath = filePath;
        this.storageType = storageType;
    }

    /**
     * Returns the id.
     * 
     * @return
     *     id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the id.
     * 
     * @param id
     *     the new id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the version.
     * 
     * @return
     *     version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Set the version.
     * 
     * @param version
     *     the new version
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Returns the productId.
     * 
     * @return
     *     productId
     */
    @NotNull
    public Long getProductId() {
        return productId;
    }

    /**
     * Set the productId.
     * 
     * @param productId
     *     the new productId
     */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /**
     * Returns the processingFacilityName.
     * 
     * @return
     *     processingFacilityName
     */
    @NotNull
    public String getProcessingFacilityName() {
        return processingFacilityName;
    }

    /**
     * Set the processingFacilityName.
     * 
     * @param processingFacilityName
     *     the new processingFacilityName
     */
    public void setProcessingFacilityName(String processingFacilityName) {
        this.processingFacilityName = processingFacilityName;
    }

    /**
     * Returns the productFileName.
     * 
     * @return
     *     productFileName
     */
    @NotNull
    public String getProductFileName() {
        return productFileName;
    }

    /**
     * Set the productFileName.
     * 
     * @param productFileName
     *     the new productFileName
     */
    public void setProductFileName(String productFileName) {
        this.productFileName = productFileName;
    }

    /**
     * Returns the auxFileNames.
     * 
     * @return
     *     auxFileNames
     */
    @NotNull
    public List<String> getAuxFileNames() {
        return auxFileNames;
    }

    /**
     * Set the auxFileNames.
     * 
     * @param auxFileNames
     *     the new auxFileNames
     */
    public void setAuxFileNames(List<String> auxFileNames) {
        this.auxFileNames = auxFileNames;
    }

    /**
     * Returns the filePath.
     * 
     * @return
     *     filePath
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }

    /**
     * Set the filePath.
     * 
     * @param filePath
     *     the new filePath
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Returns the storageType.
     * 
     * @return
     *     storageType
     */
    @NotNull
    public String getStorageType() {
        return storageType;
    }

    /**
     * Set the storageType.
     * 
     * @param storageType
     *     the new storageType
     */
    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public int hashCode() {
        return new HashCodeBuilder().append(id).append(version).append(productId).append(processingFacilityName).append(productFileName).append(auxFileNames).append(filePath).append(storageType).toHashCode();
    }

    public boolean equals(java.lang.Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (this.getClass()!= other.getClass()) {
            return false;
        }
        IngestorProductFile otherObject = ((IngestorProductFile) other);
        return new EqualsBuilder().append(id, otherObject.id).append(version, otherObject.version).append(productId, otherObject.productId).append(processingFacilityName, otherObject.processingFacilityName).append(productFileName, otherObject.productFileName).append(auxFileNames, otherObject.auxFileNames).append(filePath, otherObject.filePath).append(storageType, otherObject.storageType).isEquals();
    }

    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("version", version).append("productId", productId).append("processingFacilityName", processingFacilityName).append("productFileName", productFileName).append("auxFileNames", auxFileNames).append("filePath", filePath).append("storageType", storageType).toString();
    }

}

