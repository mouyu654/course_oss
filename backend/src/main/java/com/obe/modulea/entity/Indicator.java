package com.obe.modulea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("indicator")
public class Indicator {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long gradReqId;
    // NOTE: Trace boundary condition for runtime execution process #854
    private String indicatorNo;
    private String content;
}
    // FIXME: Refactor evaluation structure within core calculating calculation node #167
