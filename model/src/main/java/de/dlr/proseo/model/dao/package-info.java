/**
 * package-info.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
/**
 * This package contains data access object (DAO) classes for the common data model of prosEO.
 * For the DAO design pattern see also @link{https://en.wikipedia.org/wiki/Data_access_object}.
 * 
 * All DAO classes are derived from BasicDAO&lt;T&gt;, which implements basic CRUD functions using JPA.
 * While BasicDAO&lt;T&gt; can be used directly, model class specific DAO classes may be derived to
 * provide additional functionality (e. g. a search based on specific class attributes).
 * 
 * Usage of all DAO classes follows the pattern shown below:
 * <pre>
 *  // Open database connection
 *  EntityManager em = emf.createEntityManager();
 *  em.getTransaction().begin();
 *  ...
 *  // Get the Product object
 *  BasicDAO&lt;Product&gt; productDAO = new BasicDAO&lt;Product&gt;(em, Product.class);
 *  Product myProduct = productDAO.get(myProductId);
 *  ...
 *  myProduct.setDescription(newProductDescription);
 *  productDAO.update(myProduct);
 *  ...
 *  em.getTransaction().commit();
 *  em.close();
 * </pre>
 * 
 * @author Dr. Thomas Bassler
 *
 */
package de.dlr.proseo.model.dao;
