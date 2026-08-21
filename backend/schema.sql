-- ============================================================
-- 「纸页之间」参考建表脚本（MySQL 8）
-- 说明：后端默认使用 JPA ddl-auto=update 自动建表，
--       本脚本仅供手动初始化 / 迁移参考。
-- ============================================================

CREATE DATABASE IF NOT EXISTS paper_pages
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE paper_pages;

CREATE TABLE IF NOT EXISTS articles (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  slug       VARCHAR(120) NOT NULL,
  title      VARCHAR(255) NOT NULL,
  type       VARCHAR(20)  NOT NULL,
  category   VARCHAR(50)  NULL,
  date       DATE         NOT NULL,
  tags       VARCHAR(500) NULL,
  summary    TEXT         NULL,
  content    MEDIUMTEXT   NULL,
  views      INT          NOT NULL DEFAULT 0,
  status     VARCHAR(20)  NOT NULL DEFAULT 'published',
  updated_at DATE         NULL,
  created_at DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_articles_slug (slug),
  KEY idx_articles_date (date),
  KEY idx_articles_type (type),
  KEY idx_articles_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS comments (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  article_id BIGINT       NOT NULL,
  author     VARCHAR(50)  NOT NULL,
  content    TEXT         NOT NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'pending',
  date       DATE         NOT NULL,
  created_at DATETIME     NULL,
  PRIMARY KEY (id),
  KEY idx_comments_article (article_id),
  KEY idx_comments_status (status),
  CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES articles (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_users (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  username   VARCHAR(50)  NOT NULL,
  password   VARCHAR(100) NOT NULL,
  created_at DATETIME     NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_users_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article_likes (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  article_id  BIGINT      NOT NULL,
  visitor_key VARCHAR(64) NOT NULL,
  created_at  DATETIME    NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_like_article_visitor (article_id, visitor_key),
  KEY idx_likes_article (article_id),
  CONSTRAINT fk_likes_article FOREIGN KEY (article_id) REFERENCES articles (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS site_settings (
  skey  VARCHAR(50) NOT NULL,
  `value` TEXT      NOT NULL,
  PRIMARY KEY (skey)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 注意：博主账号密码由后端首次启动时自动创建（BCrypt 加密），
-- 默认 admin / admin123（可通过环境变量 ADMIN_USERNAME / ADMIN_PASSWORD 覆盖）。
