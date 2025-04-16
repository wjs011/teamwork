创建数据库的sql代码
-- 商品表

CREATE TABLE IF NOT EXISTS product (
    id VARCHAR(50) PRIMARY KEY COMMENT '京东商品ID',
    name VARCHAR(100) COMMENT '商品名称',
    url VARCHAR(255) COMMENT '商品URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';

-- 评论模板表

CREATE TABLE IF NOT EXISTS comment_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    product_id VARCHAR(50) COMMENT '商品ID',
    content TEXT COMMENT '评论内容',
    nickname VARCHAR(100) COMMENT '用户昵称',
    score INT COMMENT '评分',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论模板表';


user数据库![image](https://github.com/user-attachments/assets/d0731816-be36-494c-856e-84c15bda45a6)
