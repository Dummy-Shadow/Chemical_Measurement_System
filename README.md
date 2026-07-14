# PFEP车间冷却介质检测数据管理系统

## 项目简介

用于管理PFEP车间冷却介质（乳化液、珩磨液、清洗液、淬火液、去离子水）的日常检测数据，支持拍照上传、OCR视觉识别、自动填充、预警标色等功能。

## 技术栈

- **前端**：Vue 3 + Element Plus + Pinia + Axios + Vite
- **后端**：Spring Boot 2.7 + MyBatis-Plus + MySQL 8.0 + JWT + Knife4j
- **OCR**：百度OCR / 阿里云OCR / PaddleOCR

## 项目结构

```
Chemical_Measurement_System/
├── chemical-measurement-backend/    # Spring Boot 后端
│   ├── src/main/java/com/pfep/cms/
│   │   ├── config/                  # 配置类
│   │   ├── controller/              # 控制器
│   │   ├── service/                 # 业务层
│   │   ├── mapper/                  # 数据访问层
│   │   ├── entity/                  # 实体类
│   │   ├── dto/                     # 请求DTO
│   │   ├── vo/                      # 响应VO
│   │   ├── common/                  # 通用类（Result/异常处理）
│   │   └── util/                    # 工具类（JWT）
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-dev.yml
│   └── sql/init.sql                 # 建表脚本
├── chemical-measurement-frontend/   # Vue 3 前端
│   └── src/
│       ├── api/                     # 接口封装
│       ├── router/                  # 路由配置
│       ├── store/                   # Pinia状态管理
│       ├── views/                   # 页面组件
│       │   ├── login/               # 登录页
│       │   ├── dashboard/           # 首页
│       │   ├── upload/              # 数据上传
│       │   ├── inspection/          # 检测数据
│       │   ├── correction/          # 数据勘正
│       │   └── admin/               # 系统管理
│       ├── components/              # 公共组件
│       └── utils/                   # 工具函数
└── docs/                            # 文档
```

## 快速启动

### 1. 数据库

```bash
mysql -u root -p < chemical-measurement-backend/sql/init.sql
```

### 2. 后端

```bash
cd chemical-measurement-backend
mvn spring-boot:run
```

默认管理员账号：admin / admin123

### 3. 前端

```bash
cd chemical-measurement-frontend
npm install
npm run dev
```

访问 http://localhost:3000

## 核心功能模块

| 模块 | 说明 |
|------|------|
| 数据采集 | 拍照上传纸质检测记录，OCR识别手写数据 |
| 数据勘正 | 人工核对修正OCR识别结果 |
| 预警服务 | 自动比对检测值与预警范围，黄/红色标 |
| 复测管理 | 超差数据的复测结果录入与对照 |
| 介质配置 | 产线工位+介质牌号关联，灵活调整指标范围 |
| 查询展示 | 多条件筛选查询，列表色标展示预警状态 |
