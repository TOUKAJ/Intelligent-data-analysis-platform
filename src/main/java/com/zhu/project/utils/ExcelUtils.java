package com.zhu.project.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
public class ExcelUtils {
    public static String excelToCsv(MultipartFile multipartFile) {

        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream()).excelType(ExcelTypeEnum.XLSX).sheet().headRowNumber(0).doReadSync();
        } catch (IOException e) {
            log.info("表格处理错误",e);

        }
        if (CollUtil.isEmpty(list)) {
            return "";
        }
        //转换为csv
        StringBuilder stringBuilder = new StringBuilder();
        //读取表头
        LinkedHashMap<Integer, String> headermap = (LinkedHashMap) list.get(0);
        List<String> headerlist = headermap.values().stream().filter(header -> ObjectUtils.isNotEmpty(header)).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerlist, ",")).append("\n");
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> datamap = (LinkedHashMap) list.get(i);
            List<String> datalist = datamap.values().stream().filter(header -> ObjectUtils.isNotEmpty(header)).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(datalist, ",")).append("\n");
        }
        return stringBuilder.toString();
    }


}
