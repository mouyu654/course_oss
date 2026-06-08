# 毕业要求达成度计算平台

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

### 1. 数据库初始化

```bash
mysql -u root -p < sql/db.sql
```

### 2. 后端启动

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 http://localhost:8080

接口文档见 http://localhost:8080/swagger-ui.html

### 3. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 http://localhost:5173

开发模式下自动代理 `/api` 请求到后端。

### 4. 在线试用

访问：http://159.223.50.237/

---

## 项目结构

```
course_oss/
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/obe/platform/
│       ├── config/                   # 跨域、安全、MyBatis 配置
│       ├── security/                 # JWT 认证、RBAC 权限
│       ├── engine/                   # 核心计算引擎（三级达成度）
│       ├── modulea/                  # 基础数据与宏观管理
│       ├── moduleb/                  # 课程大纲与微观映射
│       ├── modulec/                  # 成绩录入与计算触发
│       └── moduled/                  # 报表生成与导出
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # 按角色拆分的接口封装
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
|------|------|
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
|------|------|------|
| admin | 系统管理员 | 系统管理员 |
| academic_01 | 教务管理员 | 张三 |
| director_01 | 专业负责人 | 李四 |
| teacher_01 | 主讲教师 | 王五 |

---

## 文档

- [软件需求规格说明书](docs/软件需求规格说明书.md)
- [系统架构设计](docs/系统架构设计.md)
