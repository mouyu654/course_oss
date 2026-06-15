-- ============================================================
-- 毕业要求达成度计算平台 — 数据库初始化脚本
-- Database: obe_platform
-- MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS obe_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE obe_platform;

-- ============================================================
-- 1. 权限与字典模块
-- ============================================================

-- 系统角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(20)  NOT NULL UNIQUE COMMENT '角色编码 ADMIN/ACADEMIC/DIRECTOR/TEACHER',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色显示名',
    description VARCHAR(200) COMMENT '角色描述',
    status      TINYINT      DEFAULT 1 COMMENT '状态 1启用/0禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    real_name     VARCHAR(50)  NOT NULL,
    role_id       BIGINT       NOT NULL COMMENT 'FK→sys_role.id',
    college_id    BIGINT       COMMENT 'FK→sys_college.id',
    status        TINYINT      DEFAULT 1 COMMENT '1启用/0禁用',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学期字典
CREATE TABLE IF NOT EXISTS sys_dict_semester (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    academic_year VARCHAR(9)   NOT NULL COMMENT '如2025-2026',
    semester      TINYINT      NOT NULL COMMENT '1/2',
    semester_code VARCHAR(30)  COMMENT '如2025-2026-1',
    start_date    DATE,
    end_date      DATE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学院字典
CREATE TABLE IF NOT EXISTS sys_college (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 专业字典
CREATE TABLE IF NOT EXISTS sys_major (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    code       VARCHAR(30)  UNIQUE,
    college_id BIGINT       COMMENT 'FK→sys_college.id',
    FOREIGN KEY (college_id) REFERENCES sys_college(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 行政班级
CREATE TABLE IF NOT EXISTS sys_admin_class (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_id        BIGINT      NOT NULL COMMENT 'FK→sys_major.id',
    class_name      VARCHAR(50) NOT NULL,
    enrollment_year INT         NOT NULL COMMENT '入学年份',
    UNIQUE KEY uk_major_class (major_id, class_name),
    FOREIGN KEY (major_id) REFERENCES sys_major(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 行政班级-学生关联
CREATE TABLE IF NOT EXISTS admin_class_student (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_class_id  BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    UNIQUE KEY uk_admin_class_student (admin_class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. 培养方案与宏观矩阵模块
-- ============================================================

-- 毕业要求
CREATE TABLE IF NOT EXISTS grad_requirement (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_id BIGINT       NOT NULL COMMENT 'FK→sys_major.id',
    req_no   INT          NOT NULL COMMENT '编号 1-8',
    title    VARCHAR(200) NOT NULL,
    content  TEXT,
    FOREIGN KEY (major_id) REFERENCES sys_major(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 毕业要求指标点
CREATE TABLE IF NOT EXISTS indicator (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    grad_req_id  BIGINT      NOT NULL COMMENT 'FK→grad_requirement.id',
    indicator_no VARCHAR(5)  NOT NULL COMMENT '如3-1',
    content      TEXT        NOT NULL,
    FOREIGN KEY (grad_req_id) REFERENCES grad_requirement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程库
CREATE TABLE IF NOT EXISTS course (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    code   VARCHAR(30) UNIQUE,
    name   VARCHAR(100) NOT NULL,
    credit DECIMAL(3,1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程-专业关联
CREATE TABLE IF NOT EXISTS course_major (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    major_id  BIGINT NOT NULL,
    UNIQUE KEY uk_course_major (course_id, major_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 宏观支撑矩阵
CREATE TABLE IF NOT EXISTS macro_support_matrix (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     BIGINT        NOT NULL,
    indicator_id  BIGINT        NOT NULL,
    support_level ENUM('H','M','L') COMMENT '支撑强度',
    weight        DECIMAL(5,4)  COMMENT '总支撑权重 W_c',
    UNIQUE KEY uk_course_indicator (course_id, indicator_id),
    FOREIGN KEY (course_id)    REFERENCES course(id),
    FOREIGN KEY (indicator_id) REFERENCES indicator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. 教学班级与学生模块
-- ============================================================

-- 学生
CREATE TABLE IF NOT EXISTS student (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no       VARCHAR(20) NOT NULL UNIQUE,
    name             VARCHAR(50) NOT NULL,
    college_id       BIGINT,
    major_id         BIGINT,
    enrollment_year  INT         COMMENT '入学年份',
    enrollment_status VARCHAR(20) DEFAULT '在读',
    admin_class_id   BIGINT,
    FOREIGN KEY (college_id) REFERENCES sys_college(id),
    FOREIGN KEY (major_id)   REFERENCES sys_major(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 教学班级
CREATE TABLE IF NOT EXISTS teaching_class (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT,
    teacher_id  BIGINT,
    semester_id BIGINT,
    class_name  VARCHAR(50),
    FOREIGN KEY (course_id)   REFERENCES course(id),
    FOREIGN KEY (teacher_id)  REFERENCES sys_user(id),
    FOREIGN KEY (semester_id) REFERENCES sys_dict_semester(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 教学班级-学生关联
CREATE TABLE IF NOT EXISTS class_student (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id   BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    UNIQUE KEY uk_class_student (class_id, student_id),
    FOREIGN KEY (class_id)   REFERENCES teaching_class(id),
    FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. 课程大纲与微观映射模块
-- ============================================================

-- 课程大纲 (1:1 with teaching_class)
CREATE TABLE IF NOT EXISTS course_outline (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT UNIQUE COMMENT 'FK→teaching_class.id 一对一',
    status   ENUM('DRAFT','LOCKED') DEFAULT 'DRAFT'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程目标
CREATE TABLE IF NOT EXISTS course_objective (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    outline_id  BIGINT      NOT NULL,
    obj_no      VARCHAR(10) NOT NULL COMMENT '如1-1, 2-1',
    dimension   VARCHAR(10) COMMENT '知识/能力/价值',
    description TEXT        NOT NULL,
    FOREIGN KEY (outline_id) REFERENCES course_outline(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程目标对指标点的内部贡献权重
CREATE TABLE IF NOT EXISTS objective_indicator_weight (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    objective_id BIGINT       NOT NULL,
    indicator_id BIGINT       NOT NULL,
    weight       DECIMAL(5,4) NOT NULL COMMENT 'w_jk 内部贡献权重',
    UNIQUE KEY uk_obj_indicator (objective_id, indicator_id),
    FOREIGN KEY (objective_id) REFERENCES course_objective(id),
    FOREIGN KEY (indicator_id) REFERENCES indicator(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 考核点
CREATE TABLE IF NOT EXISTS assessment_point (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    outline_id     BIGINT        NOT NULL,
    name           VARCHAR(100)  NOT NULL,
    max_score      DECIMAL(6,2)  NOT NULL COMMENT '满分',
    weight_percent DECIMAL(5,2)  COMMENT '权重百分比',
    objective_id   BIGINT        COMMENT '绑定的课程目标(单值)',
    sort_order     INT,
    FOREIGN KEY (outline_id) REFERENCES course_outline(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 考核点-课程目标 N:M 绑定
CREATE TABLE IF NOT EXISTS assessment_objective (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    objective_id  BIGINT NOT NULL,
    UNIQUE KEY uk_assess_obj (assessment_id, objective_id),
    FOREIGN KEY (assessment_id) REFERENCES assessment_point(id),
    FOREIGN KEY (objective_id)  REFERENCES course_objective(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 考核点下的题目/子项 (如期末大题1, 大题2)
CREATE TABLE IF NOT EXISTS assessment_question (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    max_score     DECIMAL(6,2) NOT NULL,
    sort_order    INT,
    FOREIGN KEY (assessment_id) REFERENCES assessment_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 题目-课程目标 N:M 绑定
CREATE TABLE IF NOT EXISTS question_objective (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    objective_id BIGINT NOT NULL,
    UNIQUE KEY uk_q_obj (question_id, objective_id),
    FOREIGN KEY (question_id) REFERENCES assessment_question(id),
    FOREIGN KEY (objective_id) REFERENCES course_objective(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. 成绩与计算结果模块
-- ============================================================

-- 成绩单主表 (1:1 with teaching_class)
CREATE TABLE IF NOT EXISTS score_sheet (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id  BIGINT UNIQUE COMMENT 'FK→teaching_class.id',
    status    ENUM('EMPTY','IMPORTED','LOCKED') DEFAULT 'EMPTY',
    locked_at DATETIME,
    locked_by BIGINT COMMENT '操作人 FK→sys_user.id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 学生考核点成绩明细
CREATE TABLE IF NOT EXISTS student_score (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      BIGINT       NOT NULL,
    student_id    BIGINT       NOT NULL,
    assessment_id BIGINT       NOT NULL,
    question_id   BIGINT       COMMENT '可选的题目级FK',
    score         DECIMAL(6,2) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sheet_student_assess (sheet_id, student_id, assessment_id, question_id),
    FOREIGN KEY (sheet_id)      REFERENCES score_sheet(id),
    FOREIGN KEY (student_id)    REFERENCES student(id),
    FOREIGN KEY (assessment_id) REFERENCES assessment_point(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 目标级达成度 (第一级)
CREATE TABLE IF NOT EXISTS obj_achievement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id     BIGINT        NOT NULL,
    objective_id BIGINT        NOT NULL,
    achievement  DECIMAL(6,4)  NOT NULL COMMENT 'C̄_j',
    calc_time    DATETIME      NOT NULL,
    UNIQUE KEY uk_class_obj (class_id, objective_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 课程级达成度 (第二级)
CREATE TABLE IF NOT EXISTS course_achievement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id     BIGINT        NOT NULL,
    indicator_id BIGINT        NOT NULL,
    achievement  DECIMAL(6,4)  NOT NULL COMMENT 'E_k',
    calc_time    DATETIME      NOT NULL,
    UNIQUE KEY uk_class_indicator (class_id, indicator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 专业级达成度 (第三级)
CREATE TABLE IF NOT EXISTS major_achievement (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    major_id        BIGINT        NOT NULL,
    indicator_id    BIGINT        NOT NULL,
    semester_id     BIGINT        COMMENT '学期',
    enrollment_year INT           COMMENT '入学年份/年级',
    achievement     DECIMAL(6,4)  NOT NULL COMMENT 'G_k',
    calc_time       DATETIME      NOT NULL,
    triggered_by    BIGINT        COMMENT '触发者 FK→sys_user.id',
    UNIQUE KEY uk_major_ind_sem (major_id, indicator_id, enrollment_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 成绩解锁工单
CREATE TABLE IF NOT EXISTS score_unlock_request (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sheet_id      BIGINT       NOT NULL,
    class_id      BIGINT       NOT NULL,
    requester_id  BIGINT       NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    status        VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    reviewer_id   BIGINT,
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    reviewed_at   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. 索引
-- ============================================================

CREATE INDEX idx_user_role       ON sys_user(role_id);
CREATE INDEX idx_student_score_sheet ON student_score(sheet_id);
CREATE INDEX idx_student_score_student ON student_score(student_id);
CREATE INDEX idx_assessment_point_outline ON assessment_point(outline_id);
CREATE INDEX idx_obj_weight_indicator ON objective_indicator_weight(indicator_id);
CREATE INDEX idx_course_achievement_indicator ON course_achievement(indicator_id);
CREATE INDEX idx_macro_matrix_indicator ON macro_support_matrix(indicator_id);
CREATE INDEX idx_class_student_class ON class_student(class_id);
CREATE INDEX idx_major_achievement_enrollment ON major_achievement(enrollment_year);
CREATE INDEX idx_teaching_class_teacher ON teaching_class(teacher_id);
CREATE INDEX idx_grad_req_major ON grad_requirement(major_id);
CREATE INDEX idx_indicator_grad_req ON indicator(grad_req_id);
CREATE INDEX idx_score_sheet_class ON score_sheet(class_id);

-- ============================================================
-- 7. 种子数据
-- ============================================================

-- 角色
INSERT INTO sys_role (role_code, role_name, description) VALUES
('ADMIN',    '系统管理员', '管理系统用户、基础字典、成绩解锁'),
('ACADEMIC', '教务管理员', '管理课程体系、学生名单、教学班级、查看宏观看板、导出报告'),
('DIRECTOR', '专业负责人', '录入毕业要求与指标点、配置宏观支撑矩阵、触发专业级计算'),
('TEACHER',  '主讲教师', '设定课程目标、分配权重、配置考核点、导入成绩、触发课程级计算');

-- 默认用户 (密码均为 123456 的 BCrypt 哈希)
INSERT INTO sys_user (username, password_hash, real_name, role_id) VALUES
('admin',       '$2b$12$N45UXFA2DDVf08iPPGVQ0exx0moUORf0hwg3UM0DNLoxsJaZStmqy', '系统管理员', 1),
('academic_01', '$2b$12$N45UXFA2DDVf08iPPGVQ0exx0moUORf0hwg3UM0DNLoxsJaZStmqy', '张三',       2),
('director_01', '$2b$12$N45UXFA2DDVf08iPPGVQ0exx0moUORf0hwg3UM0DNLoxsJaZStmqy', '李四',       3),
('teacher_01',  '$2b$12$N45UXFA2DDVf08iPPGVQ0exx0moUORf0hwg3UM0DNLoxsJaZStmqy', '王五',       4);
