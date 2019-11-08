/**
 * TaskRepository.java
 */
package de.dlr.proseo.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.dlr.proseo.model.Task;

/**
 * Data Access Object for the Task class
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

}
