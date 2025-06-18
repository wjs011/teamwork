CREATE TABLE IF NOT EXISTS comment_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    keywords TEXT NOT NULL COMMENT '分类关键词（JSON格式）',
    priority INT DEFAULT 0 COMMENT '优先级',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论分类表'; 