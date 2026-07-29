# PFEP车间冷却介质检测数据管理系统

Copyright (c) 2026 郑杭宇. Released under MIT License.

## 项目简介

用于管理PFEP车间冷却介质（乳化液、珩磨液、清洗液、淬火液、去离子水）的日常检测数据。将传统的"纸质记录→人工录入Excel→邮件分发"流程升级为"手动/拍照录入→预警自动判断→内网在线查看"。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Element Plus + Pinia + Axios + Vite + ECharts + XLSX |
| 后端 | Spring Boot 2.7 + MyBatis-Plus 3.5 + Spring Security + JWT + Knife4j |
| 数据库 | MySQL 8.0 |
| 保护 | 前端混淆 (rollup-plugin-obfuscator) + Jasypt 加密 (PBEWithMD5AndDES) + MIT License |

## 项目结构

```
Chemical_Measurement_System/
├── start.bat                          # 一键启动脚本
├── backend.bat                        # 后端启动子脚本
├── LICENSE                            # MIT License
├── README.md
├── chemical-measurement-backend/      # Spring Boot 后端 (66 源文件)
│   ├── pom.xml
│   ├── sql/init.sql                   # 完整建表+种子数据(4测试账号)
│   └── src/main/java/com/pfep/cms/
│       ├── config/      SecurityConfig, JwtAuthenticationFilter
│       ├── controller/  AdminController, AuthController, DashboardController,
│       │                ExportController, KnowledgeBaseController,
│       │                ManualEntryController, OcrController, ProfileController,
│       │                RetestController, ScheduleController, MediaController, etc.
│       ├── service/     AuthService, InspectionService, OcrService, WarningService
│       ├── mapper/      16个Mapper接口
│       ├── entity/      16个实体类
│       └── common/      Result<T>, PageResult<T>, GlobalExceptionHandler
├── chemical-measurement-frontend/     # Vue 3 前端
│   └── src/
│       ├── api/          Axios接口封装
│       ├── router/       路由 + 角色权限守卫
│       ├── store/        Pinia用户状态
│       └── views/        10+页面组件
└── docs/
```

## 核心业务数据

### 产线 → 工位 → 介质 → 指标

```
ZKG（缸盖线）→ AF260/AF330/AF340 → SK 5215GT           浓度 2.0-4.0   pH 8.8-10.5
ZK（缸体线） → AF60/AF220        → SK 5215             浓度 2.0-4.0   pH 8.8-10.5
KW（曲轴线） → AF50/AF160        → SK 5215PHZ / VR 1021 (可切换)
PL（连杆线） → AF10/AF50/AF80    → SinTex SC 1588      浓度 5.0-8.0   pH 8.6-9.8
             → AF90              → ET46 for cutting*    双组分体系
             → AF100             → SK 5215             浓度 1.5-3.5
BF（集中冷却）→ 5个工位         → 多种介质
```

*ET46双组分体系：含 ET46浓度/ET浓度/折光浓度 三项，折光浓度仅警戒值无标准限

### 预警判断

| 状态 | 条件 | 颜色 | 处理 |
|------|------|:---:|------|
| 正常 | 警戒下限 ≤ 值 ≤ 警戒上限 | 无色 | — |
| 预警 | 标准下限 ≤ 值 < 警戒下限 | 黄色 | 关注 |
| 超差 | 值 < 标准下限 或 值 > 标准上限 | 红色 | 需复测 |

## 快速启动

### 甲方交付（一键启动）

双击 `start.bat`，自动完成：环境检测 → MySQL启动 → 首次建库 → 启动后端(8090) → 启动前端(3000) → 打开浏览器

### 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 11+ | 自动检测 |
| Maven | 3.9+ | 自动检测 |
| MySQL | 8.0+ | 首次自动建库 |
| Node.js | 16+ | 自动检测 |

### 开发启动

```powershell
# MySQL
Start-Process "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" -ArgumentList "--standalone --datadir=C:\PROGRA~3\MySQL\MYSQLS~1.4\Data" -WindowStyle Minimized

# 后端
cd chemical-measurement-backend
$env:JAVA_HOME = "C:\Program Files\JetBrains\PyCharm 2024.1.4\jbr"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\ASUS\AppData\Local\Programs\maven\bin;$env:Path"
mvn spring-boot:run

# 前端
cd chemical-measurement-frontend
npm run dev
```

### 生产构建（前端JS混淆）

```bash
cd chemical-measurement-frontend
npm run build   # 输出混淆后文件到 dist/
```

## 三级权限

| 角色 | 用户名 | 密码 | 管辖 | 系统管理 | 排班 | 知识库 |
|------|--------|------|:---:|:---:|:---:|:---:|
| 开发者 | dev_admin | 123456 | 全部 | ✅ | ❌ | ✅ |
| 分区管理者 | area_mgr | 123456 | 全部 | ✅ | ✅ | ✅ 增删改 |
| 审核者A | inspector_a | 123456 | ZKG,ZK | ❌ | 查看 | 提建议 |
| 审核者B | inspector_b | 123456 | KW,PL,BF | ❌ | 查看 | 提建议 |

> **多Tab登录：** 使用 `sessionStorage` 存储Token，不同浏览器Tab可同时登录不同账号互不干扰。关闭浏览器后自动清除登录状态，下次启动回到登录页。

## 角色数据隔离

系统通过 `inspection_type` 字段实现审核者与管理者数据隔离：

| 角色 | 录入自动标记 | 可见范围 |
|------|------|------|
| 审核者 (INSPECTOR) | DAILY（日常检测） | 仅DAILY数据 |
| 管理者 (AREA_MANAGER) | SPOT_CHECK（抽检） | 全部（DAILY + SPOT_CHECK） |
| 开发者 (DEVELOPER) | NULL（无限制） | 全部 |

- 检测内容和指标模板两个角色完全一致
- 管理者抽检数据不计入日常检测统计，独立显示在仪表盘

## 系统管理

开发者/管理者可见，五个Tab：产线管理 / 工位管理 / 介质管理 / 工位介质关联 / 用户管理

- 删除用户：自动取消排班 + 清空关联
- 删除产线：级联删工位 → 关联 → 指标
- 介质修改：检测知识库关联，可选替换/删除
- 密码策略：最小8位

## 排班管理

分区管理者操作：一键排班（全部产线排同一人）/ 逐线安排 / 取消
审核者操作：查看自己排班 / 申请变更（换人/取消）→ 管理者审批
审核者仅能在已排班的产线内进行手动录入

## 知识库

三级流程：审核者提建议 → 管理者审核（采纳/拒绝，可修改建议内容）→ 生成知识条目

每条知识关联四级业务定位：产线 → 工位 → 介质 → 指标
管理者和开发者可增删改知识条目；审核者仅可提交建议

## 复测管理

支持多次复测，无次数上限：
- 录入后超差（status=3）→ 手动录入页介标注"待复测"
- 点击复测按钮 → 表单展示原异常值 → 填复测值提交
- 每次复测生成新 InspectionRecord（entryType=RETEST）
- 复测后正常/预警即锁定，不再允许对同一工位介质继续录入
- 页面展示复测链 Timeline
- 后端 entry() 和 submitRetest() 均校验 latest.status≠3 时拒绝写入

## 仪表盘

### 统计卡片（5+1列）

| 卡片 | 说明 |
|------|------|
| 检测次数 | 当日所有日常检测记录数（不按工位介质去重） |
| 已完成 | 当日有检测记录的独立工位+介质组合数 |
| 待复测 | 当日最终状态为超差（status=3）待处理的工位介质组合数 |
| 异常项目 | 当日最终状态为预警或超差的工位介质组合数 |
| 本周抽检 | 已完成抽检产线条数 / 总产线条数；全部完成显示"本周抽检已完成" |

### 图表

- 7天4线趋势图（正常/复测正常/预警/超差），排除抽检数据
- 4色饼图（同上分类）

### 本周异常清单

汇总本周全部异常项目（不分角色，日常+抽检均显示），含工位、介质、异常指标及实测值、发现方式、日期。

### 数据导出（管理者）

| 导出项 | 说明 |
|------|------|
| 审核者日常检测 | 本周全部审核者 DAILY 数据，导出为 Excel (.xlsx) |
| 管理者抽检数据 | 本周管理者本人 SPOT_CHECK 数据，导出为 Excel (.xlsx) |

导出字段：产线、工位、介质、检测人、日期、指标名、指标值、状态、检测类型

## 安全

- CORS 指定前端地址（非通配符）
- JWT 密钥优先读环境变量 `JWT_SECRET`
- DB 密码优先读环境变量 `DB_PASSWORD`
- 密码 BCrypt 加密，最小8位 + 用户名唯一
- 前端生产构建 JS 混淆 (rollup-plugin-obfuscator)
- Jasypt 依赖已集成，部署时可启用
- MIT License + 版权声明
- 文件上传：扩展名白名单 + 大小限制 + 路径安全

## 完成度

| 模块 | 状态 |
|------|:---:|
| 项目框架+前后端 | ✅ |
| 数据库 + 种子数据 | ✅ |
| 用户认证 JWT | ✅ |
| 手动录入 + 复测 | ✅ |
| 历史记录查看 | ✅ |
| 检测数据列表 + Excel导出 | ✅ |
| 仪表盘统计（独立计数+多次复测+本周异常清单） | ✅ |
| 角色数据隔离（DAILY/SPOT_CHECK） | ✅ |
| 知识库三级流程 | ✅ |
| 三级权限 + 路由守卫 | ✅ |
| 排班管理 | ✅ |
| 系统管理 CRUD + 用户管理 | ✅ |
| 操作日志查看 | ✅ |
| 管理者数据导出（Excel） | ✅ |
| 拍照上传 OCR | ⚠️ 模拟数据 |
| 级联删除保护 | ✅ |
| 一键启动交付 | ✅ |
| 前端混淆 | ✅ |
| 登录限流 | ❌ 待引入 |
| OWASP 依赖扫描 | ❌ 待配置 |
| 自动化测试 | ❌ 待编写 |
| Spring Boot 3.x 迁移 | ❌ 长期任务 |
