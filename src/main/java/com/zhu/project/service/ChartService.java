package com.zhu.project.service;
import com.zhu.project.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 朱先生
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2023-12-13 13:30:29
*/
public interface ChartService extends IService<Chart> {

    Chart getChartById(long id);


}
