
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
| 前端 | Vue 3 + Vite + Element Plus + Pinia |
| 数据库 | MySQL 8.0 |
| 认证 | Spring Security + JWT + RBAC |
| Excel | Alibaba EasyExcel |
| 报表 | Apache PDFBox + ECharts |

---

## 快速开始

### 1. 本地免环境快速启动 (Docker Compose 方案)
项目已实现工业级容器化基建。本地无需安装 Java 或 Node.js，在根目录下确保 Docker 守护进程启动，执行：

```bash
docker-compose up -d
```

### 2. 本地传统开发模式启动

#### 数据库初始化

```bash
mysql -u root -p < sql/db.sql

```

#### 后端启动

```bash
cd backend
mvn spring-boot:run

```

* 后端默认运行在：http://localhost:8080
* 接口文档地址：http://localhost:8080/swagger-ui.html

#### 前端启动

```bash
cd frontend
npm install
npm run dev

```

* 前端默认运行在：http://localhost:5173
* 开发模式下已配置 Vite 反向代理，自动路由 `/api` 请求至本地后端。

### 3. 在线生产试用

项目已通过 GitHub Actions 部署至分布式云服务器环境：
访问地址：http://159.223.50.237/

---

## 项目结构

```
course_oss/
├── .github/workflows/                 # GitHub Actions 自动化基建
│   └── ci-cd.yml                     # 跨域编译、GHCR 镜像打包、远程免密部署流水线
├── backend/                          # Spring Boot 后端
│   ├── Dockerfile                    # 基于 eclipse-temurin:17-jre-alpine 的镜像配方
│   └── src/main/java/com/obe/platform/
│       ├── config/                   # 跨域、安全、MyBatis 配置
│       ├── security/                 # JWT 认证、RBAC 权限
│       ├── engine/                   # 核心计算引擎（三级达成度）
│       ├── modulea/                  # 基础数据与宏观管理
│       ├── moduleb/                  # 课程大纲与微观映射
│       ├── modulec/                  # 成绩录入与计算触发
│       └── moduled/                  # 报表生成与导出
├── frontend/                         # Vue 3 前端
│   ├── Dockerfile                    # 基于 nginx:alpine 的多阶段构建配方
│   └── src/
│       ├── api/                      # 按角色拆分的接口封装（已修正静态构建引用冲突）
│       ├── views/admin/              # 系统管理员页面
│       ├── views/academic/           # 教务管理员页面
│       ├── views/director/           # 专业负责人页面
│       └── views/teacher/            # 主讲教师页面
├── sql/                              # 数据库脚本
├── docs/                             # 需求规格说明书、架构设计文档
└── README.md

```

---

## 系统角色

| 角色 | 职责 |
| --- | --- |
| 系统管理员 | 管理用户账号、基础数据字典（学院/专业/学期/班级） |
| 教务管理员 | 管理课程体系、学生名单、教学班级、查看宏观看板、导出报告 |
| 专业负责人 | 录入毕业要求与指标点、配置宏观支撑矩阵、触发专业级计算 |
| 主讲教师 | 设定课程目标、分配权重、配置考核点、导入成绩、触发课程级计算 |

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

## 默认账号

所有账号默认密码为 `123456`。

| 账号 | 角色 | 姓名 |
| --- | --- | --- |
| admin | 系统管理员 | 系统管理员 |
| academic_01 | 教务管理员 | 张三 |
| director_01 | 专业负责人 | 李四 |
| teacher_01 | 主讲教师 | 王五 |

---

## 文档

* [软件需求规格说明书](https://www.google.com/search?q=docs/%E8%BD%AF%E4%BB%B6%E9%9C%80%E6%B1%82%E8%A7%84%E6%A0%BC%E8%AF%B4%E6%98%8E%E4%B9%A6.md)
* [系统架构设计](https://www.google.com/search?q=docs/%E7%B3%BB%E7%BB%9F%E6%9E%B6%E6%9E%84%E8%AE%BE%E8%AE%A1.md)
