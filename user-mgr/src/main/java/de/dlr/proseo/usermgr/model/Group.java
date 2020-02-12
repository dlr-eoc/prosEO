/**
 * Group.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 * A group of users
 * 
 * @author Dr. Thomas Bassler
 */
@Entity(name = "groups")
public class Group {

	/** Database generated ID value for the group. */
	@Id
	@GeneratedValue
	private long id;
	
	/** 
	 * The (unique) name of the group, consisting of the mission code, a hyphen ("-") and the actual group name 
	 * (which may be used across missions to denote equivalent functional groups). 
	 */
	@Column(nullable = false, unique = true)
	private String groupName;
	
	/** The users belonging to this group */
	@OneToMany
	private Set<GroupMember> groupMembers = new HashSet<>();
	
	/** The authorities (privileges) members of this group are granted */
	@ElementCollection
	@CollectionTable(name = "group_authorities", joinColumns = {
			@JoinColumn(
				name = "group_id", 
				foreignKey = @ForeignKey(name = "fk_group_authorities_group")
		)})
	private Set<GroupAuthority> groupAuthorities = new HashSet<>();

	/**
	 * Gets the group ID
	 * 
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the group ID
	 * 
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the group name
	 * 
	 * @return the group name
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * Sets the group name
	 * 
	 * @param groupName the group name to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * Gets the members of this group
	 * 
	 * @return a set of users
	 */
	public Set<GroupMember> getGroupMembers() {
		return groupMembers;
	}

	/**
	 * Sets the members of this group
	 * 
	 * @param groupMembers the group members to set
	 */
	public void setGroupMembers(Set<GroupMember> groupMembers) {
		this.groupMembers = groupMembers;
	}

	/**
	 * Gets the authorities granted to members of this group
	 * 
	 * @return the group authorities
	 */
	public Set<GroupAuthority> getGroupAuthorities() {
		return groupAuthorities;
	}

	/**
	 * Sets the authorities granted to members of this group
	 * 
	 * @param groupAuthorities the group authorities to set
	 */
	public void setGroupAuthorities(Set<GroupAuthority> groupAuthorities) {
		this.groupAuthorities = groupAuthorities;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Group))
			return false;
		Group other = (Group) obj;
		return Objects.equals(groupName, other.groupName);
	}

}
