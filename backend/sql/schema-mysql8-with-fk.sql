-- Wiki schema (MySQL 8) with foreign keys
-- Path: backend/sql/schema-mysql8-with-fk.sql

CREATE DATABASE IF NOT EXISTS wiki
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE wiki;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       NOT NULL,
  username      VARCHAR(64)  NOT NULL,
  email         VARCHAR(128) NULL,
  phone         VARCHAR(32)  NULL,
  password_hash VARCHAR(128) NOT NULL,
  nickname      VARCHAR(64)  NULL,
  avatar_url    VARCHAR(512) NULL,
  role          VARCHAR(16)  NOT NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  deleted_at    DATETIME(6)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_users_username (username),
  UNIQUE KEY idx_users_email (email),
  UNIQUE KEY idx_users_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_logs (
  id          BIGINT        NOT NULL,
  user_id     BIGINT        NULL,
  username    VARCHAR(64)   NULL,
  action      VARCHAR(64)   NULL,
  target_type VARCHAR(64)   NULL,
  target_id   VARCHAR(64)   NULL,
  ip          VARCHAR(64)   NULL,
  detail      VARCHAR(1024) NULL,
  created_at  DATETIME(6)   NOT NULL,
  updated_at  DATETIME(6)   NOT NULL,
  deleted_at  DATETIME(6)   NULL,
  PRIMARY KEY (id),
  KEY idx_logs_created_at (created_at),
  KEY idx_logs_user_id (user_id),
  CONSTRAINT fk_logs_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_bases (
  id          BIGINT       NOT NULL,
  name        VARCHAR(128) NOT NULL,
  type        VARCHAR(16)  NOT NULL,
  owner_id    BIGINT       NOT NULL,
  description VARCHAR(512) NULL,
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  deleted_at  DATETIME(6)  NULL,
  PRIMARY KEY (id),
  KEY idx_kb_owner (owner_id),
  CONSTRAINT fk_kb_owner
    FOREIGN KEY (owner_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_base_members (
  id         BIGINT      NOT NULL,
  kb_id      BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,
  role       VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_kb_user (kb_id, user_id),
  KEY idx_kbm_kb (kb_id),
  KEY idx_kbm_user (user_id),
  CONSTRAINT fk_kbm_kb
    FOREIGN KEY (kb_id) REFERENCES knowledge_bases(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_kbm_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS documents (
  id               BIGINT       NOT NULL,
  kb_id            BIGINT       NOT NULL,
  parent_id        BIGINT       NULL,
  title            VARCHAR(256) NOT NULL,
  markdown_content LONGTEXT     NOT NULL,
  html_content     LONGTEXT     NOT NULL,
  owner_id         BIGINT       NOT NULL,
  visibility       VARCHAR(16)  NOT NULL,
  view_count       BIGINT       NOT NULL,
  version_no       INT          NOT NULL,
  published        TINYINT(1)   NOT NULL,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  deleted_at       DATETIME(6)  NULL,
  PRIMARY KEY (id),
  KEY idx_doc_kb (kb_id),
  KEY idx_doc_parent (parent_id),
  KEY idx_doc_deleted (deleted_at),
  CONSTRAINT fk_doc_kb
    FOREIGN KEY (kb_id) REFERENCES knowledge_bases(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_doc_parent
    FOREIGN KEY (parent_id) REFERENCES documents(id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_doc_owner
    FOREIGN KEY (owner_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS favorite_documents (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  user_id    BIGINT      NOT NULL,
  kb_id      BIGINT      NOT NULL,
  doc_id     BIGINT      NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_favorite_user_doc (user_id, doc_id),
  KEY idx_favorite_user_kb (user_id, kb_id),
  CONSTRAINT fk_fav_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_fav_kb
    FOREIGN KEY (kb_id) REFERENCES knowledge_bases(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_fav_doc
    FOREIGN KEY (doc_id) REFERENCES documents(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_versions (
  id               BIGINT       NOT NULL,
  doc_id           BIGINT       NOT NULL,
  version_no       INT          NOT NULL,
  title            VARCHAR(256) NOT NULL,
  markdown_content LONGTEXT     NOT NULL,
  html_content     LONGTEXT     NOT NULL,
  editor_id        BIGINT       NOT NULL,
  editor_name      VARCHAR(64)  NULL,
  commit_message   VARCHAR(256) NULL,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  deleted_at       DATETIME(6)  NULL,
  PRIMARY KEY (id),
  KEY idx_dv_doc (doc_id),
  CONSTRAINT fk_dv_doc
    FOREIGN KEY (doc_id) REFERENCES documents(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_dv_editor
    FOREIGN KEY (editor_id) REFERENCES users(id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_drafts (
  id               BIGINT       NOT NULL,
  doc_id           BIGINT       NOT NULL,
  user_id          BIGINT       NOT NULL,
  title            VARCHAR(256) NULL,
  markdown_content LONGTEXT     NULL,
  created_at       DATETIME(6)  NOT NULL,
  updated_at       DATETIME(6)  NOT NULL,
  deleted_at       DATETIME(6)  NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_draft_doc_user (doc_id, user_id),
  CONSTRAINT fk_draft_doc
    FOREIGN KEY (doc_id) REFERENCES documents(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_draft_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS share_links (
  id         BIGINT      NOT NULL,
  token      VARCHAR(64) NOT NULL,
  doc_id     BIGINT      NOT NULL,
  creator_id BIGINT      NOT NULL,
  expire_at  DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted_at DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY idx_share_token (token),
  KEY idx_share_doc (doc_id),
  CONSTRAINT fk_share_doc
    FOREIGN KEY (doc_id) REFERENCES documents(id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_share_creator
    FOREIGN KEY (creator_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
