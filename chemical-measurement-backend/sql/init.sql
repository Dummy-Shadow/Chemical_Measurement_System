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
    role        VARCHAR(20)  NOT NULL DEFAULT 'INSPECTOR' COMMENT '角色: DEVELOPER/AREA_MANAGER/INSPECTOR',
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

-- 知识库表
DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base (
    kb_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识条目ID',
    title        VARCHAR(200) NOT NULL COMMENT '问题标题',
    category     VARCHAR(50)  DEFAULT NULL COMMENT '分类: 浓度异常/pH异常/细菌超标/电导率异常/氯离子超标',
    media_id     BIGINT       DEFAULT NULL COMMENT '关联介质ID',
    indicator_id BIGINT       DEFAULT NULL COMMENT '关联指标ID',
    symptom      TEXT         COMMENT '异常现象描述',
    cause        TEXT         COMMENT '可能原因',
    solution     TEXT         NOT NULL COMMENT '处理措施/解决方案',
    priority     TINYINT      DEFAULT 1 COMMENT '优先级: 1-低 2-中 3-高',
    usage_count  INT          DEFAULT 0 COMMENT '引用次数',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (kb_id),
    KEY idx_category (category),
    KEY idx_media_id (media_id),
    KEY idx_usage (usage_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 默认账号（密码: 123456）
INSERT INTO user (username, password, real_name, role, managed_lines) VALUES
('dev_admin', '$2b$10$Qd1IDp76/UKTo.fIQKM1qOIP45SXMzvudRfN3YfdMhF/.6liyYitG', '开发者', 'DEVELOPER', NULL),
('area_mgr', '$2b$10$MxyNapr.dAqBvgT.r0ZTd.FpI7luEW.SIYJ8Cdtr10d8hHtXg/Z.u', '张管理', 'AREA_MANAGER', NULL),
('inspector_a', '$2b$10$dGu0igwnLP0le1624cKxYuCxEKmngixjOAp4h19fLdfGMJ1D.BvHG', '李审核', 'INSPECTOR', '1,2'),
('inspector_b', '$2b$10$dbUXmRNJmEK5Du.K4wcFIeyxHWaeazUaZCCyuH1ZvJv5Kb/GvOVMa', '王审核', 'INSPECTOR', '3,4,5');

INSERT INTO knowledge_base (title, category, symptom, cause, solution, priority) VALUES
('乳化液浓度偏低', '浓度异常', '折光浓度低于标准下限，可能导致润滑不足、工件锈蚀', '补水过多、原液补加不足、泄漏', '1.检测系统漏水点并修复；2.按比例补加原液；3.复测至合格', 3),
('乳化液浓度偏高', '浓度异常', '折光浓度高于标准上限，可能导致泡沫多、工件残留', '补水不足、原液补加过量', '1.定量补水稀释；2.充分搅拌后复测；3.调整补液比例', 2),
('pH值偏低（酸性偏移）', 'pH异常', 'pH值持续下降，可能导致设备腐蚀、细菌滋生', '细菌繁殖产酸、外来酸性污染、原液变质', '1.检测细菌含量；2.添加pH调节剂；3.严重时更换新液', 3),
('pH值偏高（碱性偏移）', 'pH异常', 'pH值持续上升，可能影响防锈性能', '碱性清洗剂混入、补水水质问题', '1.检查清洗液管路是否泄漏；2.检测补水水质；3.部分更换液体', 2),
('细菌含量超标', '细菌超标', '液体发臭、颜色变深、pH下降', '长期未杀菌、温度适宜细菌繁殖、外来污染', '1.添加杀菌剂；2.加强循环过滤；3.严重时排空消毒后更换新液', 3),
('真菌含量超标', '细菌超标', '液面出现霉斑、管路堵塞', '环境潮湿、长期停机未处理', '1.添加真菌抑制剂；2.清理过滤系统；3.加强日常维护频率', 2),
('清洗液浓度偏低', '浓度异常', '清洗效果差、工件表面残留油污', '清洗液补加不足、水分蒸发后补水过多', '1.按比例补加清洗液原液；2.调整自动补液系统参数；3.复测确认', 2),
('电导率异常偏高', '电导率异常', '电导率超出标准上限，影响冷却效能', '离子积累、外来盐类污染、补水硬度过高', '1.检测补水水质；2.部分更换液体；3.必要时进行去离子处理', 2);
