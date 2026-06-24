-- 创建话题表
CREATE TABLE IF NOT EXISTS topic (
    topic_id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100),
    title VARCHAR(200) NOT NULL,
    content TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    is_pinned BOOLEAN DEFAULT FALSE,
    is_locked BOOLEAN DEFAULT FALSE,
    reply_count INT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_course_id (course_id),
    INDEX idx_author_id (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建回复表
CREATE TABLE IF NOT EXISTS reply (
    reply_id VARCHAR(50) PRIMARY KEY,
    topic_id VARCHAR(50) NOT NULL,
    author_id VARCHAR(50) NOT NULL,
    author_name VARCHAR(100),
    content TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    create_time DATETIME,
    INDEX idx_topic_id (topic_id),
    INDEX idx_author_id (author_id),
    FOREIGN KEY (topic_id) REFERENCES topic(topic_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
