CREATE TABLE public_space_points (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     public_space_id BIGINT NOT NULL,
     points VARCHAR(255) NULL,
     CONSTRAINT FK_public_space FOREIGN KEY (public_space_id) REFERENCES public_spaces (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
