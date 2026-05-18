# 面向专业认证的毕业要求达成度统一计算平台

本项目致力于为本科工程教育认证及师范类专业认证（OBE 导向）提供基于成绩的毕业要求达成度统一自动计算平台。系统面向直接评价场景，自动聚合结构化学生成绩数据，实现从原始数据录入到最终认证数据输出的全流程自动化与可追溯。平台目标是彻底告别手工 Excel 计算，专注于高精度、规范、可追溯的毕业要求达成度结果生成。

---

## 技术栈

- **后端**：Python 3 + FastAPI
- **前端**：Vue 3 + Vite
- **架构**：前后端分离

---

## 快速开始

### 1. 后端启动

```bash
cd backend
python -m venv venv
source venv/bin/activate           # Windows 用户用 venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload
```

### 2. 前端启动

```bash
cd frontend
npm install
npm run dev
```
前端默认启动地址为 http://localhost:5173

---
