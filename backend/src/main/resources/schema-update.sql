-- 三级权限模型数据库更新脚本

-- 1. 创建部门表
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT PRIMARY KEY COMMENT '部门ID（雪花算法）',
    name VARCHAR(128) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NULL COMMENT '父部门ID（支持部门层级）',
    manager_id BIGINT NULL COMMENT '部门部长用户ID',
    description VARCHAR(512) NULL COMMENT '部门描述',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    deleted_at DATETIME(6) NULL COMMENT '删除时间（软删除）',
    INDEX idx_dept_parent (parent_id),
    INDEX idx_dept_manager (manager_id),
    FOREIGN KEY (parent_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 2. 修改用户表，添加部门字段
ALTER TABLE users
ADD COLUMN department_id BIGINT NULL COMMENT '所属部门ID' AFTER role,
ADD INDEX idx_user_dept (department_id),
ADD CONSTRAINT fk_user_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;

-- 3. 创建文档查看记录表
CREATE TABLE IF NOT EXISTS document_view_logs (
    id BIGINT PRIMARY KEY COMMENT '记录ID（雪花算法）',
    doc_id BIGINT NOT NULL COMMENT '文档ID',
    user_id BIGINT NOT NULL COMMENT '查看用户ID',
    username VARCHAR(64) NULL COMMENT '用户名（冗余）',
    ip VARCHAR(64) NULL COMMENT 'IP地址',
    user_agent VARCHAR(512) NULL COMMENT '用户代理',
    view_duration INT NULL COMMENT '查看时长（秒）',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '查看时间',
    INDEX idx_view_doc (doc_id),
    INDEX idx_view_user (user_id),
    INDEX idx_view_time (created_at),
    FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档查看记录表';

-- 4. 创建文档修改记录表
CREATE TABLE IF NOT EXISTS document_edit_logs (
    id BIGINT PRIMARY KEY COMMENT '记录ID（雪花算法）',
    doc_id BIGINT NOT NULL COMMENT '文档ID',
    user_id BIGINT NOT NULL COMMENT '修改用户ID',
    username VARCHAR(64) NULL COMMENT '用户名（冗余）',
    action VARCHAR(32) NOT NULL COMMENT '操作类型（CREATE/UPDATE/DELETE/PUBLISH）',
    title_before VARCHAR(256) NULL COMMENT '修改前标题',
    title_after VARCHAR(256) NULL COMMENT '修改后标题',
    content_length_before INT NULL COMMENT '修改前内容长度',
    content_length_after INT NULL COMMENT '修改后内容长度',
    ip VARCHAR(64) NULL COMMENT 'IP地址',
    commit_message VARCHAR(256) NULL COMMENT '提交说明',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '修改时间',
    INDEX idx_edit_doc (doc_id),
    INDEX idx_edit_user (user_id),
    INDEX idx_edit_time (created_at),
    INDEX idx_edit_action (action),
    FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档修改记录表';

-- 5. 修改知识库类型（如果已有数据，需要先备份）
-- 注意：这会影响现有数据，建议先备份
-- UPDATE knowledge_bases SET type = 'COMPANY' WHERE type = 'PUBLIC';
-- 新的类型：COMPANY（公司公开）、DEPARTMENT（部门）、PRIVATE（私有）

-- 6. 收藏表（文档收藏）
CREATE TABLE IF NOT EXISTS favorite_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_favorite_user_doc (user_id, doc_id),
    KEY idx_favorite_user_kb (user_id, kb_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_kb FOREIGN KEY (kb_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_doc FOREIGN KEY (doc_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏文档表';
