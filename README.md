# 毕业要求达成度计算平台

![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/ShaoChenHeng/course_oss/ci-cd.yml?branch=main&label=CI%2FCD&style=flat-square)
![GitHub Repo size](https://img.shields.io/github/repo-size/ShaoChenHeng/course_oss?style=flat-square&color=blue)
![Docker Container](https://img.shields.io/badge/deployment-docker--container-chartreuse?style=flat-square)
![Base Image](https://img.shields.io/badge/backend-eclipse--temurin%3A17--jre--alpine-blue?style=flat-square)
![Web Server](https://img.shields.io/badge/frontend-nginx%3Aalpine-teal?style=flat-square)

面向本科工程教育认证与师范类专业认证（OBE 导向）的毕业要求达成度统一自动计算平台。系统覆盖从课程目标设定、考核点映射、学生成绩录入到三级达成度自动计算的全流程，支持数据溯源和穿透式报表导出。

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Java 17 + Spring Boot 3.2 + MyBatis-Plus |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |
| 数据库 | MySQL 8.0 |
| 认证 | Spring Security + JWT + RBAC |
| Excel | Alibaba EasyExcel |
| 报表 | Apache PDFBox + Apache POI |

---

## 项目结构

```
course_oss/
├── .github/workflows/                 # GitHub Actions 自动化流水线
│   ├── ci-cd.yml                     # 构建、打包、部署
│   ├── ci-architecture-lint.yml      # 架构规范检查
│   ├── ci-dependency-audit.yml       # 依赖安全审计
│   └── ci-lifecycle-validation.yml   # 生命周期校验
├── backend/                          # Spring Boot 后端
│   ├── Dockerfile                    # 容器镜像配方
│   ├── pom.xml                       # Maven 依赖管理
│   └── src/main/java/com/obe/
│       ├── config/                   # 跨域、安全、MyBatis 配置
│       ├── engine/                   # 核心计算引擎（三级达成度）
│       ├── modulea/                  # 基础数据与宏观管理
│       │   ├── controller/           # Auth、User、Dict、Student、Course、GradReq、MacroMatrix 等
│       │   ├── dto/                  # 请求/响应数据传输对象
│       │   ├── entity/               # 数据库实体类
│       │   ├── mapper/               # MyBatis-Plus Mapper
│       │   └── service/              # 业务逻辑层
│       ├── moduleb/                  # 课程大纲与微观映射
│       │   ├── controller/           # Objective、Assessment、Question
│       │   ├── entity/               # Objective、Assessment、Question 实体
│       │   ├── mapper/               # 对应 Mapper
│       │   └── service/              # 对应 Service
│       ├── modulec/                  # 成绩录入与计算触发
│       │   ├── controller/           # Score、TeacherCompute
│       │   ├── dto/                  # 成绩相关 DTO
│       │   ├── entity/               # Score 实体
│       │   ├── mapper/               # Score Mapper
│       │   └── service/              # 成绩与计算 Service
│       └── moduled/                  # 报表生成与导出
│           ├── controller/           # Report、Export
│           └── service/              # PDF/Excel 报表 Service
├── frontend/                         # Vue 3 前端
│   ├── Dockerfile                    # 多阶段构建配方
│   ├── index.html                    # 入口 HTML
│   ├── package.json                  # 依赖管理
│   └── src/
│       ├── api/                      # 按角色拆分的接口封装
│       ├── assets/                   # 静态资源
│       ├── components/               # 公共组件（StatusTag 等）
│       ├── composables/              # 组合式函数（useAuth 等）
│       ├── layouts/                  # 布局组件（MainLayout）
│       ├── router/                   # 路由配置（RBAC 动态路由）
│       ├── stores/                   # Pinia 状态管理（user、permission）
│       ├── utils/                    # 工具函数（request、validate）
│       └── views/                    # 页面视图
│           ├── admin/                # 系统管理员页面（6 个）
│           ├── academic/             # 教务管理员页面（7 个）
│           ├── director/             # 专业负责人页面（4 个）
│           └── teacher/              # 主讲教师页面（6 个）
├── sql/                              # 数据库脚本
│   └── db.sql                        # 建表与初始化数据
├── docs/                             # 项目文档
│   ├── 软件需求规格说明书.md
│   ├── 系统架构设计.md
│   ├── API文档.docx
│   ├── achievement_calc_数据库操作说明.docx
│   └── 计算机科学与技术课程与毕业要求指标点支撑矩阵.xlsx
├── slides/                           # 汇报材料
├── .env.example                      # 环境变量模板
├── .gitignore                        # Git 忽略规则
├── LICENSE                           # 开源协议
└── README.md                         # 项目说明
```

---

## 系统功能

### 系统管理员

| 功能 | 说明 |
|------|------|
| 用户管理 | 创建、编辑、启用/禁用、删除用户账号 |
| 学院管理 | 维护学校学院信息 |
| 专业管理 | 维护专业信息及所属学院 |
| 学期管理 | 管理学年学期（编码、起止日期） |
| 班级管理 | 管理行政班级，支持学生分配 |
| 成绩管理 | 查看所有成绩单状态，审批解锁工单 |

### 教务管理员

| 功能 | 说明 |
|------|------|
| 课程管理 | 录入课程信息（代码、名称、学分），支持批量导入 |
| 学生管理 | 管理学生信息，支持多条件筛选和批量状态更新 |
| 教学班级 | 创建教学班级，分配课程和主讲教师 |
| 批量导入 | 批量导入学生、课程、班级学生名单 |
| 达成度监控 | 全局看板，查看课程计算进度和权重配置状态 |
| 学生达成度 | 逐级浏览（学院→专业→班级→学生），查看个人达成度报告 |

### 专业负责人

| 功能 | 说明 |
|------|------|
| 毕业要求管理 | 录入毕业要求及其指标点 |
| 宏观支撑矩阵 | 配置课程对指标点的支撑权重 |
| 专业课程关联 | 配置专业与课程的关联关系 |
| 专业级计算 | 触发专业级达成度计算，查看雷达图 |

### 主讲教师

| 功能 | 说明 |
|------|------|
| 课程目标设定 | 设定课程目标（编号、维度、描述） |
| 内部权重分配 | 配置课程目标对指标点的支撑权重 |
| 考核点映射 | 定义考核点（作业、考试等）并绑定课程目标 |
| 成绩录入 | 下载模板、上传成绩、预览成绩表 |
| 成绩勘误 | 提交成绩修改申请，等待审批 |
| 课程级计算 | 触发课程级达成度计算，查看结果并导出报告 |

---

## 核心计算模型

系统采用三级达成度计算模型，数据自下而上严格传导：

**第一级 — 目标级达成度（教师触发）**

$$C_{ij} = \frac{\sum(\text{支撑目标 } j \text{ 的考核点实际得分})}{\sum(\text{支撑目标 } j \text{ 的考核点满分})}, \quad \bar{C_j} = \frac{1}{N}\sum C_{ij}$$

**第二级 — 课程级达成度（教师触发）**

$$E_k = \sum_j (\bar{C_j} \times w_{jk}), \quad \text{约束：} \sum_j w_{jk} = 1.0$$

**第三级 — 专业级达成度（专业负责人触发）**

$$G_k = \sum (E_k \times W_c), \quad \text{约束：} \sum W_c = 1.0$$

---

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 本地免环境快速启动 (Docker Compose)

项目已实现容器化部署，本地无需安装 Java 或 Node.js：

```bash
docker-compose up -d
```

### 2. 本地开发模式启动

#### 数据库初始化

```bash
mysql -u root -p < sql/db.sql
```

#### 环境变量配置

复制 `.env.example` 为 `.env`，修改数据库密码和 JWT 密钥：

```bash
cp .env.example .env
```

#### 后端启动

```bash
cd backend
mvn spring-boot:run
```

- 后端默认运行在：http://localhost:8080

#### 前端启动

```bash
cd frontend
npm install
npm run dev
```

- 前端默认运行在：http://localhost:5173
- 开发模式下已配置 Vite 反向代理，自动路由 `/api` 请求至本地后端。

### 3. 在线试用

项目已通过 GitHub Actions 部署至云服务器：
访问地址：http://159.223.50.237/

---

## 默认账号

所有账号默认密码为 `123456`。

| 账号 | 角色 | 姓名 |
|------|------|------|
| admin | 系统管理员 | 系统管理员 |
| academic_01 | 教务管理员 | 张三 |
| director_01 | 专业负责人 | 李四 |
| teacher_01 | 主讲教师 | 王五 |

---

## 文档

- [软件需求规格说明书](docs/软件需求规格说明书.md)
- [系统架构设计](docs/系统架构设计.md)
- [API 文档](docs/API文档.docx)
- [数据库操作说明](docs/achievement_calc_数据库操作说明.docx)
- [支撑矩阵示例](docs/计算机科学与技术课程与毕业要求指标点支撑矩阵.xlsx)

---

## 开源协议

本项目采用 [MIT License](LICENSE) 开源。
