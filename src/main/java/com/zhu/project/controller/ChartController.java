package com.zhu.project.controller;
import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhu.project.annotation.AuthCheck;
import com.zhu.project.bizmq.BiMessageProducer;
import com.zhu.project.common.BaseResponse;
import com.zhu.project.common.DeleteRequest;
import com.zhu.project.common.ErrorCode;
import com.zhu.project.common.ResultUtils;
import com.zhu.project.constant.CommonConstant;
import com.zhu.project.constant.UserConstant;
import com.zhu.project.exception.BusinessException;
import com.zhu.project.exception.ThrowUtils;
import com.zhu.project.manager.AiManager;
import com.zhu.project.manager.RedisLimiterManager;
import com.zhu.project.model.chart.*;
import com.zhu.project.model.entity.Chart;
import com.zhu.project.model.entity.User;
import com.zhu.project.model.vo.BiResponse;
import com.zhu.project.service.ChartService;
import com.zhu.project.service.UserService;
import com.zhu.project.utils.ExcelUtils;
import com.zhu.project.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;


    @Resource
    private RedisLimiterManager redisLimiterManager;





    @Resource
    private BiMessageProducer biMessageProducer;


    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 删除
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }


    /**
     * 根据 id 获取
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getChartById(id);//走缓存

        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }


    /**
     * 分页获取当前用户创建的资源列表
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<IPage<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                        HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        String redisKey = chartQueryRequest.getUserId() + "_" + current + "_" + size;



        //缓存存在，直接读缓存
        if (redisTemplate.hasKey(redisKey)) {
            Page<Chart> chartPage = (Page<Chart>) redisTemplate.opsForValue().get(redisKey);
            return ResultUtils.success(chartPage);
        }


        //查数据库，建立缓存
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), getQueryWrapper(chartQueryRequest));
        redisTemplate.opsForValue().set(redisKey, chartPage,5, TimeUnit.MINUTES);
        return ResultUtils.success(chartPage);

    }


    /**
     * 智能分析异步消息队列
     * @param multipartFile
     * @param genChartByAiRequset
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChatByAiMq(@RequestPart("file") MultipartFile multipartFile,
                                                  GenChartByAiRequset genChartByAiRequset, HttpServletRequest request) {
        String name = genChartByAiRequset.getName();
        String goal = genChartByAiRequset.getGoal();
        String chartType = genChartByAiRequset.getChartType();

        //校验文件大小
        long size = multipartFile.getSize();
        String name1 = multipartFile.getOriginalFilename();
        final long ONE_MB = 1 * 1024 * 1024;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大小超过1MB");
        //检验文件后缀
        String suffix = FileUtil.getSuffix(name1);
        final List<String> validSuffix = Arrays.asList("xls", "xlsx");
        ThrowUtils.throwIf(!validSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件格式非法");

        User loginUser = userService.getLoginUser(request);
        //每个用户一个限流器
        redisLimiterManager.doRateLimit("genChatByAi_" + loginUser.getId());


        final String prmopt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:\n" +
                "分析需求:\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据:\n" +
                "{csv格式的原始数据，用，作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容(此外不要输出任何多余的开头、结尾、注释)\n" +
                "【【【【【\n" +
                "{前端Echarts V5的option配置对象json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n";

        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append(prmopt);

        //图表类型
        String usergoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            usergoal += "请使用，" + chartType;
        }

        userInput.append("分析目标：").append(usergoal).append("\n");

        //压缩数据
        String result = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("数据：").append(result).append("\n");

        //插入数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(result);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus("wait");
        chartService.save(chart);


        Long id = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(id));
        //封装返回给前端
        BiResponse biRespone = new BiResponse();
        biRespone.setChartId(id);
        return ResultUtils.success(biRespone);
    }

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        Long userId = chartQueryRequest.getUserId();
        String chartType = chartQueryRequest.getChartType();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();


        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);


        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
