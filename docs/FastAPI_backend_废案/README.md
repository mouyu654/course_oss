# 毕业要求达成度计算平台 - 后端

面向专业认证的毕业要求达成度统一计算平台，基于 FastAPI 构建。

## 技术栈

- **Python** 3.10+
- **FastAPI** 0.100+
- **SQLAlchemy** 2.0+
- **Pydantic** V2
- **MySQL** 8.0+
- **Pandas / NumPy** (计算引擎)
- **OpenPyXL** (Excel 处理)
- **bcrypt** (密码哈希)
- **python-jose** (JWT 认证)
- **Uvicorn** (ASGI 服务器)

## 项目结构

```
app/
├── __init__.py
├── main.py                 # FastAPI 应用入口
├── config.py               # 配置管理
├── database.py             # 数据库连接
├── models/                 # SQLAlchemy 数据模型
├── schemas/                # Pydantic 数据校验模型
├── routers/                # API 路由
├── services/               # 业务逻辑层
│   ├── auth_service.py
│   ├── admin_service.py
│   ├── academic_service.py
│   ├── director_service.py
│   ├── teacher_service.py
│   └── calculation_engine.py
├── dependencies/           # FastAPI 依赖注入
│   └── auth.py
├── utils/                  # 工具函数
│   └── excel_utils.py
└── middleware/             # 中间件
```

## 快速启动

1. 安装依赖: `pip install -r requirements.txt`
2. 配置环境变量: 创建 `.env` 文件（参考 `.env.example`）
3. 创建数据库: `CREATE DATABASE achievement_calc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
4. 初始化数据表: 在 `main.py` 中取消注释 `init_db()` 调用，然后运行应用
5. 运行: `uvicorn app.main:app --reload`

## 核心 API

### 认证模块
- `POST /api/auth/login` - 登录认证
- `GET /api/auth/me` - 获取当前用户信息
- `GET /api/admin/users` - 查询用户列表（admin）
- `POST /api/admin/users` - 创建用户（admin）
- `PATCH /api/admin/users/{user_id}` - 更新用户（admin）

### 教务管理模块
- `POST /api/academic/courses` - 新增课程
- `POST /api/academic/courses/import` - Excel 批量导入课程
- `GET /api/academic/courses` - 查询课程列表
- `POST /api/academic/classes` - 创建教学班
- `GET /api/academic/classes` - 查询教学班列表
- `POST /api/academic/students/import` - Excel 批量导入学生
- `GET /api/academic/calculation/progress` - 计算进度看板
- `POST /api/academic/calculate/program` - 专业级全局计算
- `POST /api/academic/unlock` - 成绩解锁
- `GET /api/academic/results/program` - 专业级结果查询
- `GET /api/academic/reports/program-excel` - 专业级报告导出

### 专业设计模块
- `POST /api/director/requirements` - 新增毕业要求
- `GET /api/director/requirements` - 查询毕业要求列表
- `POST /api/director/indicators` - 新增指标点
- `GET /api/director/indicators` - 查询指标点列表
- `GET /api/director/macro-matrix` - 查询宏观矩阵
- `POST /api/director/macro-matrix` - 提交宏观矩阵

### 教师模块
- `GET /api/teacher/objectives` - 查询课程目标
- `POST /api/teacher/objectives` - 新增课程目标
- `GET /api/teacher/internal-weights` - 查询内部权重
- `POST /api/teacher/internal-weights` - 提交内部权重
- `GET /api/teacher/assessment-items` - 查询考核点
- `POST /api/teacher/assessment-items` - 新增考核点
- `GET /api/teacher/assessment-items/{course_id}/export-template` - 导出成绩模板
- `POST /api/teacher/scores/import` - 导入成绩 Excel
- `GET /api/teacher/scores/matrix` - 成绩矩阵查询
- `POST /api/teacher/calculate` - 课程级一键计算
- `GET /api/teacher/results` - 课程级报告导出

## 计算引擎

系统采用三级达成度计算：
- **Tier 1**: 课程目标级达成度 C_bar = Sum(考核点实际得分) / Sum(考核点满分)
- **Tier 2**: 课程级指标点达成度 E_k = Sum(C_bar_j * w_jk)
- **Tier 3**: 专业级指标点达成度 G_k = Sum(E_k * W)

## 数据校验

- JWT Token 有效性 + 用户存在 + is_active=true -> 401
- RBAC 角色权限 -> 403
- 宏观矩阵列合计 |Sum(W) - 1.0| <= 0.001 -> 400
- 内部权重列合计 |Sum(w) - 1.0| <= 0.001 -> 400
- 成绩锁定检查 -> 400
- 删除依赖检查 -> 409
