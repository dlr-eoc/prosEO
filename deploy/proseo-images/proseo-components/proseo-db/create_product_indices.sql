--
-- Create indices to improve product catalogue retrieval from PRIP API
--

CREATE INDEX CONCURRENTLY IF NOT EXISTS product_parameters_values
    ON product_parameters (parameters_key, parameter_value);
    
CREATE INDEX CONCURRENTLY IF NOT EXISTS product_file_names
    ON product_file (product_file_name);
  