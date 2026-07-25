# PFEP车间冷却介质检测数据管理系统

## 项目简介

用于管理PFEP车间冷却介质（乳化液、珩磨液、清洗液、淬火液、去离子水）的日常检测数据。将传统的"纸质记录→人工录入Excel→邮件分发"流程升级为"手动/拍照录入→预警自动判断→内网在线查看"。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 (Composition API) + Element Plus + Pinia + Axios + Vite + ECharts + XLSX |
| 后端 | Spring Boot 2.7 + MyBatis-Plus 3.5 + Spring Security + JWT + Knife4j |
| 数据库 | MySQL 8.0 |
| OCR | 百度OCR / 阿里云OCR / PaddleOCR（待对接） |

---

## 项目结构

```
Chemical_Measurement_System/
├── chemical-measurement-backend/
│   ├── pom.xml
│   ├── sql/init.sql                           # 建表 + 基础数据
│   └── src/main/java/com/pfep/cms/
│       ├── ChemicalMeasurementApplication.java
│       ├── config/        SecurityConfig, JwtAuthenticationFilter, MyBatisPlusConfig, MyMetaObjectHandler
│       ├── controller/    AuthController, DashboardController, KnowledgeBaseController,
│       │                  ManualEntryController, ProductionLineController, WorkstationController, MediaController
│       ├── service/       AuthService, InspectionService, OcrService, WarningService
│       ├── mapper/        15个Mapper接口
│       ├── entity/        15个实体类
│       ├── dto/           LoginDTO, RegisterDTO, ManualEntryDTO
│       ├── vo/            LoginVO
│       ├── common/        Result<T>, PageResult<T>, GlobalExceptionHandler
│       └── util/          JwtUtil
├── chemical-measurement-frontend/
│   ├── package.json, vite.config.js, index.html
│   └── src/
│       ├── api/index.js                     # Axios接口封装
│       ├── router/index.js                  # 路由 + 权限守卫
│       ├── store/user.js                    # Pinia用户状态
│       ├── utils/request.js                 # 请求拦截器(JWT注入)
│       ├── components/layout/MainLayout.vue # 侧边栏+顶栏(按角色显示)
│       └── views/
│           ├── login/Login.vue              # 登录
│           ├── dashboard/Dashboard.vue      # 首页(统计+趋势图+知识库推荐)
│           ├── upload/ManualEntry.vue       # 手动录入(分产线→工位→介质→指标)
│           ├── upload/Upload.vue            # 拍照上传(待对接OCR)
│           ├── inspection/InspectionList.vue # 检测数据查询+导出
│           ├── inspection/InspectionDetail.vue
│           ├── inspection/Knowledge.vue     # 知识库
│           ├── inspection/HistoryView.vue   # 历史记录(只读)
│           ├── correction/Correction.vue    # 数据勘正(占位)
│           └── admin/                       # 系统管理
│               ├── ProductionLine.vue, Workstation.vue, Media.vue, IndicatorConfig.vue
│               └── ScheduleManagement.vue   # 排班管理(占位)
└── .gitignore, README.md
```

---

## 数据库设计

### 核心业务表（11张）

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| production_line | 产线 | ZKG/ZK/KW/PL/BF 5条 |
| workstation | 工位 | AF260/AF330/.../ZKG加工中心等18个 |
| media_category | 介质类别 | 乳化液/清洗液/淬火液/去离子水 |
| media | 介质牌号 | SK 5215GT/ET46 for cutting等8种 |
| indicator_template | 指标模板 | 浓度/pH/防锈性/细菌/真菌/电导率/氯离子/ET46浓度/ET浓度/折光浓度等23个 |
| workstation_media | 工位-介质关联 | 20条关联 |
| workstation_media_indicator | 工位介质指标范围 | 107条，含标准上下限+警戒上下限 |
| inspection_record | 检测主记录 | 产线/工位/介质/日期/预警状态/录入方式 |
| inspection_indicator_value | 检测指标明细 | 每个指标的实测值+预警状态 |
| retest_record | 复测记录 | 超差后复测结果 |
| knowledge_base | 知识库 | 常见问题+处理措施 |

### 权限相关表（4张）

| 表名 | 说明 |
|------|------|
| user | 用户表（含role + managed_lines） |
| operation_log | 操作日志 |
| schedule | 排班表 |
| schedule_change_request | 变更申请表 |

---

## 完整业务数据

### 产线 → 工位 → 介质 → 指标

```
ZKG（缸盖线）
├─ AF260 ─── SK 5215GT          浓度 2.0-4.0    pH 8.8-10.5   防锈性≤2
├─ AF330 ─── SK 5215GT          浓度 2.0-4.0    pH 8.8-10.5   防锈性≤2
└─ AF340 ─── SK 5215GT          浓度 2.0-4.0    pH 8.8-10.5   防锈性≤2   杂质≤50mg   颗粒≤600μm

ZK（缸体线）
├─ AF60  ─── SK 5215            浓度 2.0-4.0    pH 8.8-10.5   防锈性≤2
└─ AF220 ─── SK 5215            浓度 2.0-4.0    pH 8.8-10.5   防锈性≤2   含油量≤0.3%   杂质≤50mg   颗粒≤3000μm

KW（曲轴线）
├─ AF50  ─── SK 5215PHZ / VR 1021  (两个介质可切换)
├─ AF60  ─── AQUATENSID BW [淬火液]  浓度 5.0-9.0   pH 8.6-10.0   防锈性≤2   细菌≤10⁴   真菌≤10³
└─ AF160 ─── SK 5215PHZ / VR 1021

PL（连杆线）
├─ AF10/AF50 ─── SinTex SC 1588  浓度 5.0-8.0    pH 8.6-9.8   防锈性/细菌/真菌/电导率/氯离子
├─ AF80  ─── SinTex SC 1588      浓度 7.5-11.0   pH 8.6-9.8
├─ AF90  ─── ET46 for cutting*   双组分体系(见下)
└─ AF100 ─── SK 5215             浓度 1.5-3.5    pH 8.8-10.5

BF（集中冷却）
├─ ZKG加工中心/ZKG专机 ─── ET46 for cutting*    ET46 3.0-5.0   ET 2.0-3.0   pH 8.6-9.5
├─ ZKG珩磨 ─── SinTex SC 1856    浓度 8.0-12.0   pH 8.3-9.8   细菌≤10⁵
├─ ZK集中冷却 ─── ET46 for cutting*             ET46 6.0-9.0   ET 2.0-3.0   pH 8.6-9.5
└─ PL集中冷却 ─── SinTex SC 1588  浓度 6.0-8.0    pH 8.7-9.8
```

**\*ET46双组分体系：** 含ET46浓度/ET浓度/折光浓度三项，折光浓度仅警戒值无标准限

### 预警判断规则

| 状态 | 判断条件 | 颜色 | 处理 |
|------|---------|:---:|------|
| 正常 | 警戒下限 ≤ 检测值 ≤ 警戒上限 | 无色 | — |
| 预警 | 标准下限 ≤ 检测值 < 警戒下限 | 黄色 | 提示关注 |
| 超差 | 检测值 < 标准下限 或 检测值 > 标准上限 | 红色 | 需复测 |

---

## 快速启动

### 环境要求

- JDK 11+（本项目复用 PyCharm 自带的 JDK 17）
- Maven 3.9+
- MySQL 8.0+
- Node.js 16+

### 0. 启动 MySQL（每次开机后需手动执行）

```powershell
Start-Process "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" -ArgumentList "--standalone --datadir=C:\PROGRA~3\MySQL\MYSQLS~1.4\Data" -WindowStyle Minimized
```

### 1. 数据库

```bash
# 首次运行：创建数据库并导入表结构+基础数据
mysql -u root -p < chemical-measurement-backend/sql/init.sql

# 或使用 Python 执行（本环境使用方式）
python C:\temp\import_full.py           # 导入工位介质指标范围
python C:\temp\setup_permissions.py     # 创建权限表+测试账号
python C:\temp\gen_5day.py              # 生成5天历史测试数据
```

数据库连接配置在 `application-dev.yml`：
```
spring.datasource.url: jdbc:mysql://localhost:3306/chemical_measurement?...&allowPublicKeyRetrieval=true
spring.datasource.username: root
spring.datasource.password: admin123
```

### 2. 后端

```powershell
cd D:\Chemical_Measurement_System\chemical-measurement-backend
$env:JAVA_HOME = "C:\Program Files\JetBrains\PyCharm 2024.1.4\jbr"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\ASUS\AppData\Local\Programs\maven\bin;$env:Path"
mvn spring-boot:run
```

启动成功标志：`Started ChemicalMeasurementApplication` → 端口 `8090`

API文档：`http://localhost:8090/doc.html`

### 3. 前端

```powershell
cd D:\Chemical_Measurement_System\chemical-measurement-frontend
npm install   # 仅首次
npm run dev
```

访问：`http://localhost:3000`（或自动切换端口）

---

## 三级权限

| 角色 | 用户名 | 密码 | 管辖范围 | 录入住指标 | 系统管理 | 排班管理 | 知识库编辑 |
|------|------|------|---------|:---:|:---:|:---:|:---:|
| 开发者 | `dev_admin` | 123456 | 全部 | 全部 | ✅ | ❌ | ✅ |
| 分区管理者 | `area_mgr` | 123456 | 全部产线 | 全部 | ❌ | ✅ | 采纳/拒绝建议 |
| 审核者A | `inspector_a` | 123456 | ZKG, ZK | 仅pH+浓度 | ❌ | 查看 | 提建议 |
| 审核者B | `inspector_b` | 123456 | KW, PL, BF | 仅pH+浓度 | ❌ | 查看 | 提建议 |

### 多开测试

同一台电脑同时登录多个账号测试：
- Chrome 无痕窗口 `Ctrl+Shift+N` + Edge 普通窗口
- 或不同浏览器各登一个，JWT token 独立存储互不干扰

---

## 排班管理

| 操作 | 分区管理者 | 单日审核者 | 说明 |
|------|:---:|:---:|------|
| 创建排班 | ✅ | ❌ | 手动逐线安排或一键排班（全部产线排给同一人） |
| 取消排班 | ✅ | ❌ | 带确认弹窗，产线状态变为 CANCELLED |
| 查看排班 | 全部 | 仅自己 | 审核者"我的排班"页 |
| 提交变更申请 | ❌ | ✅ | 选换人/取消，填写理由 |
| 审批变更 | ✅ | ❌ | 通过则更新排班，拒绝则原排班不变 |
| 变更历史 | ❌ | ✅ | 审核者看到自己的申请状态（待审/已通过/已拒绝） |

**排班与手动录入联动：** 审核者登录后，手动录入页只显示**今日排班中包含的产线**。无排班时显示"今日暂无排班任务"。

### 排班约束

- 一张表 `schedule`，唯一键 `UNIQUE(line_id, schedule_date)` — 每条产线每天只有一位审核者
- 取消/变更不删记录，通过 status 字段管理状态（ACTIVE / CANCELLED / CHANGED）
- 变更申请表 `schedule_change_request`，审批通过自动更新排班

---

## 知识库

### 三级协作流程

```
开发者 → [直接添加] → knowledge_base (source_type=DIRECT)
管理者 → [直接添加] → knowledge_base (source_type=DIRECT)
管理者 → [采纳建议] → knowledge_base (source_type=SUGGESTION) + 回复审核者
审核者 → [提交建议] → knowledge_suggestion (status=PENDING)
```

### 数据关联

每条知识/建议关联四级业务定位：`产线 → 工位 → 介质 → 指标`。管理者采纳时可修改审核者的建议内容（最终决策权在管理者），并附回复说明。

| 操作 | 开发者 | 分区管理者 | 单日审核者 |
|------|:---:|:---:|:---:|
| 查看知识条目 | ✅ | ✅ | ✅ |
| 新增知识条目 | ✅ | ✅ | ❌ |
| 编辑知识条目 | ✅ | ✅ | ❌ |
| 删除知识条目 | ✅ | ❌ | ❌ |
| 提交知识建议 | ❌ | ❌ | ✅ |
| 查看待审批建议 | ❌ | ✅ | ❌ |
| 采纳/拒绝建议 | ❌ | ✅ | ❌ |
| 查看自己建议及回复 | ❌ | ❌ | ✅ |

### 仪表盘统计

首页统计卡片按 **(工位, 介质, 日期)** 分组，判断每条记录的状态：

| 状态 | 条件 | 颜色 |
|------|------|:---:|
| 正常 | 仅1条记录且正常 | 绿 |
| 复测正常 | 最早异常 + 最新正常 | 蓝 |
| 预警 | 最新记录为预警 | 黄 |
| 超差 | 最新记录为超差 | 红 |

趋势图和饼图同时展示四种分类。

---

## 系统管理

开发者/管理者可见。五个Tab：

| Tab | 功能 | 谁可操作 |
|------|------|:---:|
| 产线管理 | 增删改产线（删除级联删除工位+指标） | 开发者/管理者 |
| 工位管理 | 增删改工位（选产线），删除级联删除关联 | 同上 |
| 介质管理 | 增删改介质（选类别），含关联知识库处理 | 同上 |
| 工位介质关联 | 绑定/解绑介质，**编辑各指标的标准/警戒值** | 同上 |
| 用户管理 | 增删改用户（按角色限制字段） | 开发者/管理者 |

### 介质修改关联处理

编辑/删除介质时如有关联知识库 → 弹窗选择：仅更新 / 替换为新介质 / 一并删除知识库

### 用户管理权限

| 操作 | 开发者 | 管理者 | 审核者 |
|------|:---:|:---:|:---:|
| 查看用户列表 | ✅ | ✅ | ✅ |
| 新增用户 | ✅（任意角色）| ✅（仅审核者）| ❌ |
| 编辑自己 | ✅（全部字段）| ✅（姓名/密码）| ✅（姓名/密码）|
| 编辑他人 | ✅（全部字段）| ❌ | ❌ |
| 修改他人角色 | ✅ | ❌ | ❌ |
| 删除用户 | ✅ | ❌ | ❌ |

### 个人信息修改

顶栏"个人信息"按钮，所有角色可修改自己姓名和密码。修改密码需验证旧密码。

### 级联删除保护

| 操作 | 级联影响 |
|------|---------|
| 删除用户 | 取消所有排班，清空检测记录创建人，删除账号 |
| 删除产线 | 级联删除所有工位→工位介质关联→指标配置 |
| 删除工位 | 级联删除工位介质关联→指标配置 |
| 删除介质 | 弹窗选择：替换为新介质 / 一并删除知识库 |

---

## 复测管理

超差后复测流程：

```
手动录入 → 介质标签"待复测" → 点击"复测"按钮
  → 复测表单（原异常值红显 + 标准范围对照）
    → 填写复测值 → 提交
      → 页面保留 → 复测链 Timeline 展示原始→复测全流程
```

| 特性 | 说明 |
|------|------|
| 原记录保留 | 复测生成新 inspection_record，retest_record 关联两者 |
| 多次复测 | 持续异常可反复复测，标签"复测N次，异常" |
| 复测正常 | 正常后标签"复测N次，正常"，锁定不可再录入 |
| 仪表盘联动 | 最早异常+最新正常 → 自动判定为"复测正常"(蓝色) |

---

---

## 拍照上传OCR

完整前端流程：拍照 -> 压缩上传 -> OCR识别 -> 字段展示 -> 手动修正 -> 提交。

后端 OcrController 当前返回模拟数据，对接真实API步骤：

1. 申请百度OCR/阿里云OCR Key
2. 修改 callOcrApi() 调用真实API
3. 修改 parseOcrResult() 按字段映射识别结果
4. 在 application.yml 配置API密钥

内网环境可用 PaddleOCR 本地部署：pip install paddleocr -> Java HTTP调用。

---

## 完成度

| 模块 | 状态 | 说明 |
|------|:---:|------|
| 项目框架+前后端 | ✅ | Spring Boot + Vue 3，GitHub已推送 |
| 数据库(16张表) | ✅ | 业务表+权限表+排班表+知识建议表+测试数据 |
| 用户认证(JWT) | ✅ | 登录/角色权限，JWT含userId+role，个人信息修改 |
| 手动录入 | ✅ | 分产线→工位→介质→指标，预警判断，录入锁定，复测锁定 |
| 历史记录查看 | ✅ | 只读，选产线→工位→介质→日期查看过往记录 |
| 检测数据列表 | ✅ | 分页+筛选+Excel导出 |
| 仪表盘 | ✅ | 5统计卡片+7天4线趋势+4色饼图+知识库推荐 |
| 知识库 | ✅ | 三级流程：建议提交→采纳/拒绝→入库，四级业务定位 |
| 三级权限 | ✅ | 侧边栏/菜单/录入按角色过滤 + 前端路由守卫 |
| 排班管理 | ✅ | 一键排班+逐线安排+变更申请+审批+与录入联动 |
| 复测管理 | ✅ | 复测表单+Timeline链+多次复测+仪表盘联动 |
| 系统管理CRUD | ✅ | 产线/工位/介质增删改 + 工位介质关联 + 指标范围编辑 |
| 用户管理UI | ✅ | 增删改用户+按角色限制+旧密码验证+级联删除保护 |
| 拍照上传OCR | ⚠️ | 完整流程+模拟数据，待对接真实API（百度/阿里云/PaddleOCR） |
| 操作日志查看 | ✅ | 开发者看全部，管理者看自己+审核者，审核者不可见 |

## 已知问题修复记录

| 问题 | 修复 |
|------|------|
| JWT过滤器重复注册导致请求过滤两次 | 去掉 `@Component`，仅SecurityConfig手动注册 |
| `getUserId()` 写死返回 `1L` 导致所有用户看到同一份排班 | 改为从 SecurityContext 取真实 userId |
| 数据库唯一约束 `(inspector_id,line_id,date)` 导致同一产线可排多人 | 改为 `UNIQUE(line_id, schedule_date)` |
| `toISOString()` 使用UTC时区，凌晨显示昨天日期 | 全改为本地时间 |
| 审批通过后先CANCELLED再ACTIVE两步update产生竞态 | 改为一步直接更新 |
| 知识库缺少产线/工位定位 | 加 line_id/station_id 字段，四级业务联动 |
| 仪表盘未区分复测正常 | 按(工位,介质)分组，最早异常+最新正常=复测正常 |
| `hasRole("ADMIN")` 残留导致系统管理页403 | 改为 `hasAnyRole("DEVELOPER","AREA_MANAGER")` |
| axios `baseURL` + `/api/retest` 双层前缀 → 404 | 去掉多余 `/api/` 前缀 |
| 删除用户/产线/工位无级联处理 | 添加级联取消排班/清空外键/删除子表 |
| 审核者无法修改个人信息 | 独立 `/api/profile` 接口，所有角色可访问 |

| 问题 | 修复 |
|------|------|
| JWT过滤器重复注册导致请求过滤两次 | 去掉 `@Component`，仅SecurityConfig手动注册 |
| `getUserId()` 写死返回 `1L` 导致所有用户看到同一份排班 | 改为从 SecurityContext 取真实 userId |
| 数据库唯一约束 `(inspector_id,line_id,date)` 导致同一产线可排多人 | 改为 `UNIQUE(line_id, schedule_date)` |
| `toISOString()` 使用UTC时区，凌晨显示昨天日期 | 全改为本地时间 |
| 审批通过后先CANCELLED再ACTIVE两步update产生竞态 | 改为一步直接更新 |
| 知识库缺少产线/工位定位 | 加 line_id/station_id 字段，四级业务联动 |
| 仪表盘未区分复测正常 | 按(工位,介质)分组，最早异常+最新正常=复测正常 |
| `hasRole("ADMIN")` 残留导致系统管理页403 | 改为 `hasAnyRole("DEVELOPER","AREA_MANAGER")` |
| axios `baseURL` + `/api/retest` 双层前缀 → 404 | 去掉多余 `/api/` 前缀 |

---

Copyright (c) 2026 ֣����. Released under MIT License.
