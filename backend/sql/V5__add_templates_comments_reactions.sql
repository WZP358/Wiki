-- Document Templates
CREATE TABLE IF NOT EXISTS document_templates (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    kb_id BIGINT,
    creator_id BIGINT NOT NULL,
    markdown_content LONGTEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    use_count INT NOT NULL DEFAULT 0,
    category VARCHAR(64),
    cover_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_template_kb (kb_id),
    INDEX idx_template_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Comments
CREATE TABLE IF NOT EXISTS document_comments (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    like_count INT NOT NULL DEFAULT 0,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by BIGINT,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_comment_doc (document_id),
    INDEX idx_comment_author (author_id),
    INDEX idx_comment_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Reactions
CREATE TABLE IF NOT EXISTS document_reactions (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_reaction_doc (document_id),
    INDEX idx_reaction_user (user_id),
    UNIQUE KEY uk_doc_user_reaction (document_id, user_id, reaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default templates
INSERT INTO document_templates (id, name, description, creator_id, markdown_content, is_public, category, use_count)
VALUES
(1, '会议纪要', '标准会议记录模板', 1, '# 会议纪要\n\n**会议时间：** \n**会议地点：** \n**参会人员：** \n**会议主题：** \n\n## 会议内容\n\n### 讨论事项\n1. \n\n### 决议事项\n1. \n\n### 待办事项\n- [ ] \n\n## 备注\n', TRUE, '会议', 0),
(2, '项目计划', '项目规划文档模板', 1, '# 项目计划\n\n## 项目概述\n**项目名称：** \n**项目负责人：** \n**开始时间：** \n**预计完成时间：** \n\n## 项目目标\n\n## 项目范围\n\n## 里程碑\n| 阶段 | 目标 | 时间 | 负责人 |\n|------|------|------|--------|\n| | | | |\n\n## 风险评估\n\n## 资源需求\n', TRUE, '项目', 0),
(3, '技术文档', '技术文档标准模板', 1, '# 技术文档\n\n## 概述\n\n## 技术架构\n\n## 核心功能\n\n### 功能1\n**描述：** \n**实现方式：** \n\n```javascript\n// 代码示例\n```\n\n## API接口\n\n### 接口名称\n- **URL：** \n- **方法：** \n- **参数：** \n- **返回值：** \n\n## 部署说明\n\n## 常见问题\n', TRUE, '技术', 0),
(4, '产品需求文档', 'PRD标准模板', 1, '# 产品需求文档 (PRD)\n\n## 文档信息\n**产品名称：** \n**版本号：** \n**创建日期：** \n**负责人：** \n\n## 需求背景\n\n## 用户画像\n\n## 功能需求\n\n### 需求1\n**优先级：** P0/P1/P2\n**描述：** \n**验收标准：** \n\n## 非功能需求\n\n## 设计稿\n\n## 时间规划\n', TRUE, '产品', 0),
(5, '周报模板', '个人/团队周报模板', 1, '# 周报 - 第X周\n\n**姓名：** \n**部门：** \n**日期：** \n\n## 本周工作总结\n\n### 完成事项\n1. \n\n### 进行中事项\n1. \n\n## 下周工作计划\n1. \n\n## 遇到的问题\n\n## 需要的支持\n', TRUE, '汇报', 0);
