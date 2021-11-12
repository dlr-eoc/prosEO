--
-- Create a view to find the processing facilities in component products
--
-- The recursive part checks for cycles in the product tree (which should not occur, but we play it safe here)
--
-- Adapted from the PostgreSQL documentation (https://www.postgresql.org/docs/11/queries-with.html#QUERIES-WITH-SELECT)
--

CREATE OR REPLACE VIEW product_processing_facilities AS
WITH RECURSIVE available_facilities(product_id, enclosing_product_id, processing_facility_id, depth, path, cycle) AS (
    SELECT p.id,
      p.enclosing_product_id,
      pf.processing_facility_id,
      1,
      ARRAY[p.id],
      false
    FROM product p LEFT OUTER JOIN product_file pf ON p.id = pf.product_id
  UNION
    SELECT p.id,
      p.enclosing_product_id,
      CASE WHEN af.processing_facility_id IS NULL THEN pf.processing_facility_id ELSE af.processing_facility_id END,
      af.depth + 1,
      path || p.id,
      p.id = ANY(path)
    FROM product p
    JOIN available_facilities af ON p.id = af.enclosing_product_id
    LEFT OUTER JOIN product_file pf ON p.id = pf.product_id
    WHERE (af.processing_facility_id IS NULL OR pf.processing_facility_id IS NULL OR af.processing_facility_id = pf.processing_facility_id)
      AND NOT cycle
)
SELECT * FROM available_facilities WHERE processing_facility_id IS NOT NULL;
