-- ============================================
-- PFEP车间冷却介质检测数据管理系统 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS chemical_measurement
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE chemical_measurement;

-- 用户表
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    user_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    role        VARCHAR(20)  NOT NULL DEFAULT 'INSPECTOR' COMMENT '角色: ADMIN/INSPECTOR',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删 1-已删',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 产线表
DROP TABLE IF EXISTS production_line;
CREATE TABLE production_line (
    line_id     BIGINT      NOT NULL AUTO_INCREMENT COMMENT '产线ID',
    line_code   VARCHAR(20) NOT NULL COMMENT '产线编码: ZKG/ZK/KW/PL/BF',
    line_name   VARCHAR(50) NOT NULL COMMENT '产线名称: 缸盖线/缸体线/曲轴线/连杆线',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (line_id),
    UNIQUE KEY uk_line_code (line_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产线表';

-- 工位表
DROP TABLE IF EXISTS workstation;
CREATE TABLE workstation (
    station_id   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '工位ID',
    station_code VARCHAR(50) NOT NULL COMMENT '工位编码: AF260/AF330/...',
    station_name VARCHAR(100) DEFAULT NULL COMMENT '工位名称',
    line_id      BIGINT      NOT NULL COMMENT '所属产线ID',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (station_id),
    UNIQUE KEY uk_station (line_id, station_code),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位表';

-- 介质类别表
DROP TABLE IF EXISTS media_category;
CREATE TABLE media_category (
    category_id   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '类别ID',
    category_name VARCHAR(50)  NOT NULL COMMENT '类别名称',
    description   VARCHAR(255) DEFAULT NULL COMMENT '说明',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='介质类别表';

-- 介质牌号表
DROP TABLE IF EXISTS media;
CREATE TABLE media (
    media_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '介质ID',
    media_code  VARCHAR(100) NOT NULL COMMENT '介质牌号编码',
    media_name  VARCHAR(200) DEFAULT NULL COMMENT '介质牌号名称',
    category_id BIGINT       NOT NULL COMMENT '所属类别ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (media_id),
    UNIQUE KEY uk_media_code (media_code),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='介质牌号表';

-- 指标模板表（按介质类别定义需要检测的指标项）
DROP TABLE IF EXISTS indicator_template;
CREATE TABLE indicator_template (
    template_id    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    category_id    BIGINT       NOT NULL COMMENT '介质类别ID',
    indicator_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    indicator_unit VARCHAR(20)  DEFAULT NULL COMMENT '指标单位: %/?s/cm/ppm等',
    sort_order     INT          DEFAULT 0 COMMENT '排序',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (template_id),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标模板表';

-- 工位介质关联表
DROP TABLE IF EXISTS workstation_media;
CREATE TABLE workstation_media (
    wm_id       BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    station_id  BIGINT   NOT NULL COMMENT '工位ID',
    media_id    BIGINT   NOT NULL COMMENT '介质ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (wm_id),
    UNIQUE KEY uk_station_media (station_id, media_id),
    KEY idx_station_id (station_id),
    KEY idx_media_id (media_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位介质关联表';

-- 工位介质指标范围表
DROP TABLE IF EXISTS workstation_media_indicator;
CREATE TABLE workstation_media_indicator (
    wmi_id        BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    wm_id         BIGINT         NOT NULL COMMENT '工位介质关联ID',
    indicator_id  BIGINT         NOT NULL COMMENT '指标模板ID',
    standard_min  DECIMAL(10,4)  DEFAULT NULL COMMENT '标准下限',
    standard_max  DECIMAL(10,4)  DEFAULT NULL COMMENT '标准上限',
    warn_min      DECIMAL(10,4)  DEFAULT NULL COMMENT '预警下限',
    warn_max      DECIMAL(10,4)  DEFAULT NULL COMMENT '预警上限',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (wmi_id),
    UNIQUE KEY uk_wm_indicator (wm_id, indicator_id),
    KEY idx_wm_id (wm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位介质指标范围表';

-- 检测主记录表
DROP TABLE IF EXISTS inspection_record;
CREATE TABLE inspection_record (
    record_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    station_id      BIGINT       NOT NULL COMMENT '工位ID',
    media_id        BIGINT       NOT NULL COMMENT '介质ID',
    inspection_date DATE         NOT NULL COMMENT '检测日期',
    photo_url       VARCHAR(500) DEFAULT NULL COMMENT '原始照片URL',
    inspector_id    BIGINT       DEFAULT NULL COMMENT '检测员ID',
    ocr_confidence  DOUBLE       DEFAULT NULL COMMENT 'OCR整体置信度(0-1)',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待勘正 1-已确认 2-已预警 3-已超差',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    KEY idx_station_date (station_id, inspection_date),
    KEY idx_status (status),
    KEY idx_date (inspection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测主记录表';

-- 检测指标明细表
DROP TABLE IF EXISTS inspection_indicator_value;
CREATE TABLE inspection_indicator_value (
    detail_id       BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    record_id       BIGINT        NOT NULL COMMENT '检测记录ID',
    indicator_id    BIGINT        NOT NULL COMMENT '指标模板ID',
    ocr_value       DECIMAL(10,4) DEFAULT NULL COMMENT 'OCR识别值',
    corrected_value DECIMAL(10,4) DEFAULT NULL COMMENT '人工勘正值',
    final_value     DECIMAL(10,4) DEFAULT NULL COMMENT '最终取值',
    warn_status     TINYINT       NOT NULL DEFAULT 0 COMMENT '预警状态: 0-正常 1-预警 2-超差',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (detail_id),
    KEY idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测指标明细表';

-- 复测记录表
DROP TABLE IF EXISTS retest_record;
CREATE TABLE retest_record (
    retest_id     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '复测ID',
    record_id     BIGINT       NOT NULL COMMENT '原检测记录ID',
    retest_date   DATE         NOT NULL COMMENT '复测日期',
    retest_values TEXT         NOT NULL COMMENT '复测值(JSON: 指标ID:复测值)',
    created_by    BIGINT       DEFAULT NULL COMMENT '录入人ID',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (retest_id),
    KEY idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复测记录表';

-- ============================================
-- 初始化基础数据
-- ============================================

-- 介质类别
INSERT INTO media_category (category_name, description) VALUES
('乳化液与珩磨液', '集中冷却和单机冷却用乳化液、珩磨液'),
('清洗液', 'PFEP清洗液'),
('淬火液', '淬火液'),
('去离子水', '去离子水');

-- 产线
INSERT INTO production_line (line_code, line_name) VALUES
('ZKG', '缸盖线'),
('ZK', '缸体线'),
('KW', '曲轴线'),
('PL', '连杆线'),
('BF', '表面处理线');

-- 指标模板（乳化液与珩磨液）
INSERT INTO indicator_template (category_id, indicator_name, indicator_unit, sort_order) VALUES
(1, '浓度', '%', 1),
(1, 'pH值', '', 2),
(1, '防锈性', '', 3),
(1, '细菌', '', 4),
(1, '真菌', '', 5),
(1, '电导率', 'μs/cm', 6),
(1, '氯离子', 'ppm', 7);

-- 指标模板（清洗液）
INSERT INTO indicator_template (category_id, indicator_name, indicator_unit, sort_order) VALUES
(2, 'pH值', '', 1),
(2, '浓度', '%', 2),
(2, '防锈性', '', 3),
(2, '含油量', '%', 4),
(2, '杂质', 'mg', 5),
(2, '最大颗粒长度', 'μm', 6);

-- 指标模板（淬火液）
INSERT INTO indicator_template (category_id, indicator_name, indicator_unit, sort_order) VALUES
(3, 'pH值', '', 1),
(3, '浓度', '%', 2),
(3, '防锈性', '', 3),
(3, '细菌', '', 4),
(3, '真菌', '', 5);

-- 指标模板（去离子水）
INSERT INTO indicator_template (category_id, indicator_name, indicator_unit, sort_order) VALUES
(4, '电导率', 'μs/cm', 1),
(4, '氯离子', 'ppm', 2);

-- 管理员账号（密码: admin123）
INSERT INTO user (username, password, real_name, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN');
