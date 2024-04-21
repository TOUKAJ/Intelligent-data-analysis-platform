package com.zhu.project.service.impl;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhu.project.common.ErrorCode;
import com.zhu.project.constant.CommonConstant;
import com.zhu.project.exception.BusinessException;
import com.zhu.project.model.chart.ChartQueryRequest;
import com.zhu.project.model.entity.Chart;
import com.zhu.project.service.ChartService;
import com.zhu.project.mapper.ChartMapper;
import com.zhu.project.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * @author 朱先生
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2023-12-13 13:30:29
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Autowired
    private RedisTemplate redisTemplate;

    final static String chartKey = "chart";


    @Override
    public Chart getChartById(long id) {
        String key = chartKey + id;

        //从redis中查询缓存
        Chart chart = JSONUtil.toBean((String) redisTemplate.opsForValue().get(key), Chart.class);

        //判断redis缓存是否击中
        if (chart.getId() != null) {
            return chart;
        }

        //缓存未命中
        chart = this.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        //数据库查询成功就写入缓存
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(chart),5, TimeUnit.MINUTES);
        return chart;
    }


}





