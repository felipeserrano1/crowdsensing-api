CREATE TABLE visits (
    id BIGINT PRIMARY KEY,
    center VARCHAR(255) NOT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    user_id INT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
