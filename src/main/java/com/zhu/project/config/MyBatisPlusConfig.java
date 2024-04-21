package com.zhu.project.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 */
@Configuration
@MapperScan("com.zhu.project.mapper")
public class MyBatisPlusConfig {

    /**
     * 拦截器配置
     * @return
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {

            MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();

            PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();

            paginationInnerInterceptor.setOverflow(false);
            paginationInnerInterceptor.setMaxLimit(500L);
            mybatisPlusInterceptor.addInnerInterceptor(paginationInnerInterceptor);

            return mybatisPlusInterceptor;


    }
}