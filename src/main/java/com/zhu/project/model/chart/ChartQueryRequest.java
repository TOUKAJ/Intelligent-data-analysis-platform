package com.zhu.project.model.chart;

import com.zhu.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    private String name;
    /**
     * id
     */
    private Long id;
    /**
     * 分析目标
     */
    private String goal;
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}