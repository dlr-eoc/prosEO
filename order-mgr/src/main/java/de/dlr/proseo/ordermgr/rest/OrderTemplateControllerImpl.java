/**
 * OrderTemplateControllerImpl.java
 *
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ordermgr.rest;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.rest.OrdertemplateController;
import de.dlr.proseo.model.rest.model.RestOrderTemplate;

/**
 * Spring MVC controller for the prosEO OrderTemplate Manager; implements the services required to manage processing orders
 *
 * @author Ranjitha Vignesh
 */
@Component
public class OrderTemplateControllerImpl implements OrdertemplateController {

  /** A logger for this class */
  private static ProseoLogger logger = new ProseoLogger(OrderTemplateControllerImpl.class);

  /** HTTP utility class */
  private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.ORDER_MGR);

  /** The processing order manager */
  @Autowired
  private OrderTemplateMgr orderTemplateManager;

  /** JPA entity manager */
  @PersistenceContext
  private EntityManager em;

  /**
   * Create an order from the given JSON object
   *
   * @param restOrderTemplate the JSON object to create the order from
   * @return HTTP status "CREATED" and a response containing a JSON object corresponding to the order after persistence (with ID
   *         and version for all contained objects) or HTTP status "FORBIDDEN" and an error message, if a cross-mission data
   *         access was attempted, or HTTP status "BAD_REQUEST", if any of the input data was invalid
   */
  @Override
  public ResponseEntity<RestOrderTemplate> createOrderTemplate(RestOrderTemplate restOrderTemplate) {
    if (logger.isTraceEnabled())
      logger.trace(">>> createOrderTemplate({})", (null == restOrderTemplate ? "MISSING" : restOrderTemplate.getName()));

    try {
      return new ResponseEntity<>(orderTemplateManager.createOrderTemplate(restOrderTemplate), HttpStatus.CREATED);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  /**
   * List of all orders filtered by mission, identifier, productClasses, starttime range
   *
   * @param mission           the mission code
   * @param identifier        the unique order identifier string
   * @param productClasses    an array of product types
   * @param startTimeFrom     earliest sensing start time
   * @param startTimeTo       latest sensing start time
   * @param executionTimeFrom earliest order execution time
   * @param executionTimeTo   latest order execution time
   * @return HTTP status "OK" and a list of products or HTTP status "NOT_FOUND" and an error message, if no products matching the
   *         search criteria were found, or HTTP status "FORBIDDEN" and an error message, if a cross-mission data access was
   *         attempted
   */
  @Override
  public ResponseEntity<List<RestOrderTemplate>> getOrderTemplates(String mission, String identifier, String[] productClasses) {
    if (logger.isTraceEnabled())
      logger.trace(">>> getOrderTemplates({}, {}, {}, {}, {})", mission, identifier, productClasses);

    try {
      return new ResponseEntity<>(orderTemplateManager.getOrderTemplates(mission, identifier, productClasses, null, null, null), HttpStatus.OK);
    } catch (NoResultException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Retrieve a list of orders satisfying the selection parameters
   *
   * @param mission       the mission code
   * @param identifier    the unique order identifier string
   * @param state         an array of order states
   * @param productClass  an array of product types
   * @param startTimeFrom earliest sensing start time
   * @param startTimeTo   latest sensing start time
   * @param recordFrom    first record of filtered and ordered result to return
   * @param recordTo      last record of filtered and ordered result to return
   * @param orderBy       an array of strings containing a column name and an optional sort direction (ASC/DESC), separated by
   *                      white space
   * @return the result list
   */
  @Override
  public ResponseEntity<List<RestOrderTemplate>> getAndSelectOrderTemplates(String mission, String identifier,
      String[] productClass, Long recordFrom, Long recordTo, String[] orderBy) {
    if (logger.isTraceEnabled())
      logger.trace(">>> getAndSelectOrderTemplates({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})", mission, identifier,
          productClass, recordFrom, recordTo, orderBy);

    try {
      List<RestOrderTemplate> list = orderTemplateManager.getOrderTemplates(mission, identifier, productClass,
          recordFrom, recordTo, orderBy);

      return new ResponseEntity<>(list, HttpStatus.OK);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (Exception e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Calculate the amount of orders satisfying the selection parameters. Mission code is mandatory.
   *
   * @param mission       the mission code
   * @param identifier    the unique order identifier string
   * @param state         an array of order states
   * @param productClass  an array of product types
   * @param startTimeFrom earliest sensing start time
   * @param startTimeTo   latest sensing start time
   * @param recordFrom    first record of filtered and ordered result to return
   * @param recordTo      last record of filtered and ordered result to return
   * @param orderBy       an array of strings containing a column name and an optional sort direction (ASC/DESC),separated by
   *                      white space
   * @return The order count
   */
  @Override
  public ResponseEntity<String> countSelectOrderTemplates(String mission, String identifier, String[] productClass,
       Long recordFrom, Long recordTo, String[] orderBy) {
    if (logger.isTraceEnabled())
      logger.trace(">>> countSelectOrderTemplates({}, {}, {}, {}, {}, {})", mission, identifier,
          productClass, recordFrom, recordTo, orderBy);

    try {
      String count = orderTemplateManager.countOrderTemplates(mission, identifier, productClass,
          recordFrom, recordTo, orderBy);

      return new ResponseEntity<>(count, HttpStatus.OK);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (Exception e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Find the order with the given ID
   *
   * @param id the ID to look for
   * @return HTTP status "OK" and a JSON object corresponding to the found order or HTTP status "FORBIDDEN" and an error message,
   *         if a cross-mission data access was attempted, or HTTP status "NOT_FOUND", if no orbit with the given ID exists
   */
  @Override
  public ResponseEntity<RestOrderTemplate> getOrderTemplateById(Long id) {
    if (logger.isTraceEnabled())
      logger.trace(">>> getOrderTemplateById({})", id);
    try {
      return new ResponseEntity<>(orderTemplateManager.getOrderTemplateById(id), HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (NoResultException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Delete an order by ID
   *
   * @param id the ID of the order to delete
   * @return a response entity with HTTP status "NO_CONTENT", if the deletion was successful, or HTTP status "NOT_FOUND" and an
   *         error message, if the orbit did not exist, or HTTP status "FORBIDDEN" and an error message, if a cross-mission data
   *         access was attempted, or HTTP status "NOT_MODIFIED" and an error message, if the deletion was unsuccessful
   */
  @Override
  public ResponseEntity<?> deleteOrderTemplateById(Long id) {

    if (logger.isTraceEnabled())
      logger.trace(">>> deleteOrderTemplateById({})", id);

    try {
      orderTemplateManager.deleteOrderTemplateById(id);
      return new ResponseEntity<>(new HttpHeaders(), HttpStatus.NO_CONTENT);
    } catch (EntityNotFoundException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    } catch (RuntimeException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_MODIFIED);
    }
  }

  /**
   * Update the order with the given ID with the attribute values of the given JSON object.
   *
   * @param id        the ID of the order to update
   * @param restOrderTemplate a JSON object containing the modified (and unmodified) attributes
   * @return a response containing HTTP status "OK" and a JSON object corresponding to the order after modification (with ID and
   *         version for all contained objects) or HTTP status "NOT_MODIFIED" and the unchanged order, if no attributes were
   *         actually changed, or HTTP status "NOT_FOUND" and an error message, if no order with the given ID exists, or HTTP
   *         status "FORBIDDEN" and an error message, if a cross-mission data access was attempted
   */
  // TODO To be Tested
  @Override
  public ResponseEntity<RestOrderTemplate> modifyOrderTemplate(Long id, @Valid RestOrderTemplate restOrderTemplate) {
    if (logger.isTraceEnabled())
      logger.trace(">>> modifyOrderTemplate({})", id);
    try {
      RestOrderTemplate changedOrderTemplate = orderTemplateManager.modifyOrderTemplate(id, restOrderTemplate);
      HttpStatus httpStatus = (restOrderTemplate.getVersion() == changedOrderTemplate.getVersion() ? HttpStatus.NOT_MODIFIED : HttpStatus.OK);
      return new ResponseEntity<>(changedOrderTemplate, httpStatus);
    } catch (EntityNotFoundException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.NOT_FOUND);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (ConcurrentModificationException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.CONFLICT);
    } catch (SecurityException e) {
      return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Count orders filtered by mission, identifier and id not equal nid.
   *
   * @param mission    The mission code
   * @param identifier The unique order identifier string
   * @param nid        The ids of orbit(s) found has to be unequal to nid
   * @return The number of orders found
   */
  @Transactional(isolation = Isolation.REPEATABLE_READ, readOnly = true)
  @Override
  public ResponseEntity<String> countOrderTemplates(String mission, String identifier) {
    if (logger.isTraceEnabled())
      logger.trace(">>> contOrderTemplates{}");

    // Find using search parameters
    String jpqlQuery = "select count(x) from OrderTemplate x ";
    String divider = " where ";
    if (mission != null) {
      jpqlQuery += divider + " x.mission.code = :mission";
      divider = " and ";
    }
    if (null != identifier) {
      jpqlQuery += divider + " x.name = :identifier";
      divider = " and ";
    }
    Query query = em.createQuery(jpqlQuery);
    if (null != mission) {
      query.setParameter("mission", mission);
    }
    if (null != identifier) {
      query.setParameter("identifier", identifier);
    }

    Object resultObject = query.getSingleResult();
    if (resultObject instanceof Long) {
      return new ResponseEntity<>(((Long) resultObject).toString(), HttpStatus.OK);
    }
    if (resultObject instanceof String) {
      return new ResponseEntity<>((String) resultObject, HttpStatus.OK);
    }

    return new ResponseEntity<>("0", HttpStatus.OK);
  }

}