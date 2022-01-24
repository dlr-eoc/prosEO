--
-- Insert text values for service states into mon_service_state
--

INSERT into mon_service_state (id, VERSION, NAME)
VALUES
    (1, 1, 'running'),
    (2, 1, 'stopped'),
    (3, 1, 'starting'),
    (4, 1, 'stopping'),
    (5, 1, 'degraded');
