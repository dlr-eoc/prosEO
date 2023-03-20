--
-- Create a view to find the processing facilities in component products
--
-- The recursive part checks for cycles in the product tree (which should not occur, but we play it safe here)
--
-- Adapted from the PostgreSQL documentation (https://www.postgresql.org/docs/11/queries-with.html#QUERIES-WITH-SELECT)
--

CREATE OR REPLACE VIEW public.product_processing_facilities AS
 SELECT p.id AS product_id,
    p.enclosing_product_id,
    pf.processing_facility_id,
    1 AS depth,
    ARRAY[p.id] AS path,
    false AS cycle
   FROM product p
     JOIN product_file pf ON p.id = pf.product_id;