package com.obe.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("sys_dict_semester")
public class SysDictSemester {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String academicYear;
    private Integer semester;
    private String semesterCode;
    private LocalDate startDate;
    private LocalDate endDate;
}
