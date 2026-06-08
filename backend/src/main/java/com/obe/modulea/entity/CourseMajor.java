package com.obe.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_major")
public class CourseMajor {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long majorId;
}
