
package de.dlr.proseo.api.auxipmon.rest.model;

import java.io.Serializable;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class RestInterfaceStatus implements Serializable
{

    final static long serialVersionUID = 1127253573470163878L;
    /**
     * Interface identifier
     * 
     */
    protected String id;
    /**
     * Flag indicating whether the interface endpoint is reachable
     * 
     */
    protected Boolean available;
    /**
     * Latest download performance in MiB/s
     * 
     */
    protected Double performance;

    /**
     * Creates a new RestInterfaceStatus.
     * 
     */
    public RestInterfaceStatus() {
        super();
    }

    /**
     * Creates a new RestInterfaceStatus.
     * 
     */
    public RestInterfaceStatus(String id, Boolean available, Double performance) {
        super();
        this.id = id;
        this.available = available;
        this.performance = performance;
    }

    /**
     * Returns the id.
     * 
     * @return
     *     id
     */
    @NotNull
    public String getId() {
        return id;
    }

    /**
     * Set the id.
     * 
     * @param id
     *     the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the available.
     * 
     * @return
     *     available
     */
    @NotNull
    public Boolean getAvailable() {
        return available;
    }

    /**
     * Set the available.
     * 
     * @param available
     *     the new available
     */
    public void setAvailable(Boolean available) {
        this.available = available;
    }

    /**
     * Returns the performance.
     * 
     * @return
     *     performance
     */
    @NotNull
    public Double getPerformance() {
        return performance;
    }

    /**
     * Set the performance.
     * 
     * @param performance
     *     the new performance
     */
    public void setPerformance(Double performance) {
        this.performance = performance;
    }

    public int hashCode() {
        return new HashCodeBuilder().append(id).append(available).append(performance).toHashCode();
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (this.getClass()!= other.getClass()) {
            return false;
        }
        RestInterfaceStatus otherObject = ((RestInterfaceStatus) other);
        return new EqualsBuilder().append(id, otherObject.id).append(available, otherObject.available).append(performance, otherObject.performance).isEquals();
    }

    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("available", available).append("performance", performance).toString();
    }

}
