CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    phone_number VARCHAR(25) NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    theme_preference VARCHAR(16) NULL,
    time_zone VARCHAR(64) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_phone_number (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NULL,
    category VARCHAR(60) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    due_date DATE NULL,
    completed BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tasks_user_created_at (user_id, created_at),
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS study_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    description VARCHAR(2000) NULL,
    session_date DATE NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    reminder_enabled BOOLEAN NOT NULL,
    reminder_minutes INT NOT NULL,
    completed BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_study_session_user_date (user_id, session_date),
    CONSTRAINT fk_study_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL,
    reference_id BIGINT NULL,
    is_read BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_user_read (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(2048) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    p256dh_key VARCHAR(256) NOT NULL,
    auth_key VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_subscriptions_endpoint (endpoint),
    KEY idx_push_subscription_user (user_id),
    CONSTRAINT fk_push_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_notification_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    notifications_enabled BOOLEAN NOT NULL,
    reminder_minutes INT NOT NULL,
    sound_enabled BOOLEAN NOT NULL,
    daily_reminder_enabled BOOLEAN NOT NULL,
    daily_reminder_time TIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_notification_settings_user (user_id),
    CONSTRAINT fk_user_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
