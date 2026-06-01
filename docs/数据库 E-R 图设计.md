## 1. 概述

本文档描述 achievement_calc的实体-关系（E-R）模型。

---

## 2. 实体清单及属性

| 实体名 | 中文名 | 主要属性 | 说明 |
|--------|--------|---------|------|
| User | 用户 | id (PK), username, password_hash, role, display_name, college, major, is_active | 系统用户，角色分 admin/academic/director/teacher |
| GlobalConfig | 全局配置 | id (PK), category, key, value, is_active | 数据字典 |
| Course | 课程 | id (PK), code (唯一), name, credit, hours_theory, hours_lab, college, major, academic_year, semester, is_active | 开课课程 |
| Class | 教学班 | id (PK), name, academic_year, semester, course_id (FK), teacher_id (FK) | 课程下的具体教学班，指定主讲教师 |
| Student | 学生 | id (PK), student_id (唯一学号), name, college, major, grade, is_active | 独立的学生主实体，不再绑定教学班 |
| ClassStudent | 选课关系 | id (PK), class_id (FK), student_id (FK) | 关联实体，实现 Student 与 Class 的多对多关系 |
| GraduationRequirement | 毕业要求 | id (PK), code, title, description | 一级毕业要求 |
| GraduationIndicator | 指标点 | id (PK), code, description, requirement_id (FK) | 二级指标点 |
| MacroSupportMatrix | 宏观支撑矩阵 | id (PK), course_id (FK), indicator_id (FK), weight_W | 课程对指标点的支撑权重 |
| CourseObjective | 课程目标 | id (PK), course_id (FK), code, description, dimension | 课程的分目标 |
| InternalWeightMatrix | 内部权重矩阵 | id (PK), objective_id (FK), indicator_id (FK), weight_w | 课程目标对指标点的贡献权重 |
| AssessmentItem | 考核点 | id (PK), course_id (FK), objective_id (FK), name, max_score | 考核项目 |
| StudentScore | 学生成绩 | id (PK), class_student_id (FK), assessment_item_id (FK), score | 外键改为引用 ClassStudent，每个选课记录对每个考核点一条成绩 |
| CalculationResult | 计算结果快照 | id (PK), course_id (FK), class_id (FK), level, objective_id (FK), indicator_id (FK), achievement_value, locked | 达成度计算结果 |
| UnlockLog | 解锁日志 | id (PK), course_id (FK), class_id (FK), operator_id (FK), reason | 解锁审计记录 |

所有实体均包含 created_at 和 updated_at，图中省略。

---

## 3. 实体间关系

| 关系 | 实体对 | 基数 | 外键/说明 |
|------|--------|:---:|---------|
| 主讲 | User → Class | 1 : N | classes.teacher_id → users.id |
| 开设 | Course → Class | 1 : N | classes.course_id → courses.id，级联删除 |
| 选课 | Class ↔ Student | M : N | 通过 ClassStudent 关联：一个学生可选多门课，一个班级可有多名学生 |
| 包含 | Class → ClassStudent | 1 : N | class_students.class_id → classes.id |
| 参与 | Student → ClassStudent | 1 : N | class_students.student_id → students.id |
| 细分 | GraduationRequirement → GraduationIndicator | 1 : N | indicators.requirement_id，级联删除 |
| 宏观支撑 | Course ↔ GraduationIndicator | M : N | 通过 MacroSupportMatrix（带 weight_W） |
| 课程目标 | Course → CourseObjective | 1 : N | objectives.course_id，级联删除 |
| 内部权重 | CourseObjective ↔ GraduationIndicator | M : N | 通过 InternalWeightMatrix（带 weight_w） |
| 考核设置 | Course → AssessmentItem | 1 : N | items.course_id，级联删除 |
| 考核绑定 | CourseObjective → AssessmentItem | 1 : N | items.objective_id，级联删除 |
| 成绩录入 | ClassStudent ↔ AssessmentItem | M : N | 通过 StudentScore（带 score），外键 class_student_id 指向 class_students.id |
| 结果快照 | Course → CalculationResult | 1 : N | 可空外键 |
| 结果快照 | Class → CalculationResult | 1 : N | 可空外键 |
| 结果快照 | CourseObjective → CalculationResult | 1 : N | 可空外键 |
| 结果快照 | GraduationIndicator → CalculationResult | 1 : N | 可空外键 |
| 审计记录 | Course, Class, User → UnlockLog | N : 1 : 1 | 解锁操作记录 |

---

## 4. E-R 图

```mermaid
erDiagram
    User {
        int id
        string username
        string password_hash
        string role
        string display_name
        string college
        string major
        boolean is_active
    }

    GlobalConfig {
        int id
        string category
        string key
        string value
        boolean is_active
    }

    Course {
        int id
        string code
        string name
        float credit
        int hours_theory
        int hours_lab
        string college
        string major
        string academic_year
        string semester
        boolean is_active
    }

    TeachingClass {
        int id
        int course_id
        int teacher_id
        string name
        string academic_year
        string semester
    }

    Student {
        int id
        string student_id
        string name
        string college
        string major
        string grade
        boolean is_active
    }

    ClassStudent {
        int id
        int class_id
        int student_id
    }

    CourseObjective {
        int id
        int course_id
        string code
        string description
        string dimension
    }

    AssessmentItem {
        int id
        int course_id
        int objective_id
        string name
        float max_score
    }

    StudentScore {
        int id
        int class_student_id
        int assessment_item_id
        float score
    }

    GraduationRequirement {
        int id
        string code
        string title
        string description
    }

    GraduationIndicator {
        int id
        int requirement_id
        string code
        string description
    }

    MacroSupportMatrix {
        int id
        int course_id
        int indicator_id
        float weight_W
    }

    InternalWeightMatrix {
        int id
        int objective_id
        int indicator_id
        float weight_w
    }

    CalculationResult {
        int id
        int course_id
        int class_id
        string level
        int objective_id
        int indicator_id
        float achievement_value
        boolean locked
    }

    UnlockLog {
        int id
        int course_id
        int class_id
        int operator_id
        string reason
    }

    User ||--o{ TeachingClass : "主讲"
    Course ||--o{ TeachingClass : "开设"
    TeachingClass ||--o{ ClassStudent : "包含"
    Student ||--o{ ClassStudent : "参与"
    Course ||--o{ CourseObjective : "课程目标"
    CourseObjective ||--o{ AssessmentItem : "考核绑定"
    ClassStudent ||--o{ StudentScore : "成绩录入"
    AssessmentItem ||--o{ StudentScore : "考核绑定"
    GraduationRequirement ||--o{ GraduationIndicator : "细分"
    GraduationIndicator ||--o{ MacroSupportMatrix : "宏观支撑"
    Course ||--o{ MacroSupportMatrix : "宏观支撑"
    CourseObjective ||--o{ InternalWeightMatrix : "内部权重"
    GraduationIndicator ||--o{ InternalWeightMatrix : "内部权重"
    Course ||..o{ CalculationResult : "结果快照"
    TeachingClass ||..o{ CalculationResult : "结果快照"
    CourseObjective ||..o{ CalculationResult : "结果快照"
    GraduationIndicator ||..o{ CalculationResult : "结果快照"
    CalculationResult ||..o{ UnlockLog : "审计记录"
    User ||..o{ UnlockLog : "操作人"
```

---

## 5. 设计要点说明

### 学生实体独立化

- students 表仅存储学生基本档案，学号 student_id 全局唯一。
- 姓名、学院、专业等不再因选修多门课而冗余，更新只需动一处。

### 多对多选课关系

- 通过 class_students 关联 students 和 classes，唯一约束 (class_id, student_id) 确保同一学生不会在同一班级重复添加。
- 学生名单导入时，先维护 students 表（Upsert），再向 class_students 插入选课关系。

### 成绩锚点重构

- student_scores.class_student_id 直接引用 class_students.id，将成绩精确绑定到"某学生在某班级"这一选课记录。
- 解决了原设计中同一学生选修不同班级时成绩关联歧义的问题，确保成绩计算来源唯一。

### 关联查询路径

- 查找某学生所有成绩：students → class_students → student_scores。
- 查找某班级某学生成绩：classes → class_students → student_scores。
- 达成度计算时，可通过 class_students 筛选有效选课学生。


---

## 6. 索引策略与业务规则摘要

| 表 | 关键索引 | 用途 |
|------|---------|------|
| students | UNIQUE(student_id) | 学号全局唯一，支持 Upsert |
| class_students | UNIQUE(class_id, student_id), INDEX(student_id) | 选课唯一约束，按学生查所有课程 |
| student_scores | UNIQUE(class_student_id, assessment_item_id) | 成绩 Upsert，外键 class_student_id |

- **学生导入**：先 Upsert students，再 Upsert class_students。
- **成绩导入**：通过学号+班级ID找到 class_student_id，再 Upsert student_scores。
- **锁定与解锁**：calculation_results 的 locked 字段控制成绩修改，解锁写入 unlock_log。
- **删除保护**：应用层检查下游依赖，存在时返回 409。
- **矩阵替换**：宏观支撑矩阵采用事务内 DELETE + INSERT 全量替换。
