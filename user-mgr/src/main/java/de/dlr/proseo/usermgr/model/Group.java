/**
 * Group.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.usermgr.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

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
	
	/** The name of the group. */
	@Column(nullable = false, unique = true)
	private String groupName;
	
	/** The users belonging to this group */
	@ManyToMany(mappedBy = "groups")
	private Set<User> groupMembers;
	
	/** The authorities (privileges) members of this group are granted */
	@ElementCollection
	private Set<GroupAuthority> groupAuthorities;

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
	public Set<User> getGroupMembers() {
		return groupMembers;
	}

	/**
	 * Sets the members of this group
	 * 
	 * @param groupMembers the group members to set
	 */
	public void setGroupMembers(Set<User> groupMembers) {
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

}
