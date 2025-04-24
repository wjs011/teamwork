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

--管理员表
CREATE TABLE `admin` (
`id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
`username` varchar(50) NOT NULL COMMENT '用户名',
`password` varchar(255) NOT NULL COMMENT '密码',
`nick_name` varchar(50) DEFAULT NULL COMMENT '昵称',
`age` int(3) DEFAULT NULL COMMENT '年龄',
`gender` varchar(10) DEFAULT NULL COMMENT '性别',
`address` varchar(255) DEFAULT NULL COMMENT '地址',
`avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
`token` varchar(255) DEFAULT NULL COMMENT '认证令牌',
`token_expire_time` datetime DEFAULT NULL COMMENT '令牌过期时间',
`auth_code` varchar(100) DEFAULT NULL COMMENT '认证码',
`permissions` text DEFAULT NULL COMMENT '权限列表(JSON格式)',
PRIMARY KEY (`id`),
UNIQUE KEY `idx_username` (`username`) COMMENT '用户名唯一索引',
UNIQUE KEY `idx_token` (`token`) COMMENT '令牌唯一索引',
KEY `idx_auth_code` (`auth_code`) COMMENT '认证码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员信息表';
ALTER TABLE `admin` ADD INDEX `idx_nick_name` (`nick_name`);
ALTER TABLE `admin` ADD INDEX `idx_token_expire` (`token_expire_time`);
INSERT INTO `admin` (
`username`,
`password`,
`nick_name`,
`age`,
`gender`,
`address`,
`avatar`,
`auth_code`,
`permissions`
) VALUES (
'admin',
'$2a$10$xVCHq5vJj5Z7W7Q8QYgZ.eqfB7s7XvRtjKX7cD3d9JkX1Yb9qL9XK', -- 加密后的密码'123456'
'系统管理员',
30,
'male',
'北京市海淀区',
'https://example.com/avatars/default.png',
'ADMIN123456',
'["*"]' -- 拥有所有权限
);