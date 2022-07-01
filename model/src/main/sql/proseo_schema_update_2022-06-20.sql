--
-- prosEO Database Schema Update
--
-- Date: 2022-06-20
--

--
-- Allow long product file templates in product_class (was VARCHAR(255))
--

ALTER TABLE product_class ALTER product_file_template TYPE text;

