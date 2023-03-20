INSERT INTO users (username, password, enabled)
SELECT 'PTM-testuser', '$2a$12$Er6kZMr4w/qcFivFJvGtD.QZBf9ajjTE/ib9lWSqKKtOheCNbtFxG', true
WHERE NOT EXISTS (
   SELECT username FROM users WHERE username = 'PTM-testuser'
);

DELETE FROM authorities;

INSERT INTO authorities VALUES 
('PTM-testuser', 'ROLE_PRODUCT_READER_ALL'), 
('PTM-testuser', 'ROLE_PRODUCT_INGESTOR'), 
('PTM-testuser', 'ROLE_PRODUCT_GENERATOR'), 
('PTM-testuser', 'ROLE_PRODUCT_MGR');