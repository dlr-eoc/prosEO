/**
 * GroupMember.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Stores the association between users and groups
 * 
 * @author Dr. Thomas Bassler
 */
@Entity
@Table(name = "group_members")
public class GroupMember {

	/** Database generated unique ID for this association entry. */
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	@JoinColumn(name = "username", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_group_members_user"))
	private User user;

	@ManyToOne
	@JoinColumn(name = "group_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_group_members_group"))
	private Group group;

	/**
	 * Gets the database id of this association entry
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the database id of this association entry
	 * 
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the user side of this association entry
	 * 
	 * @return the user
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Sets the user side of this association entry
	 * 
	 * @param user the user to set
	 */
	public void setUser(User user) {
		this.user = user;
	}

	/**
	 * Gets the group side of this association entry
	 * 
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	/**
	 * Sets the group side of this association entry
	 * 
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	public int hashCode() {
		return Objects.hash(group, user);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof GroupMember))
			return false;
		GroupMember other = (GroupMember) obj;
		return Objects.equals(group, other.group) && Objects.equals(user, other.user);
	}

}