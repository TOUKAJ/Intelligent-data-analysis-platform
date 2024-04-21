package com.zhu.project.model.chart;

import lombok.Data;

import java.io.Serializable;

@Data
public class GenChartByAiRequset implements Serializable {
    private String name;
    private String goal;
    private String chartType;
    private static final long serialVersionUID = 1L;
}
