package com.zhu.project.config;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@ConfigurationProperties(prefix = "spring.redis")
@Configuration
@Data
public class RedissonConfig {


    private int  database;

    private String port;

    private String host;

    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password);
        RedissonClient redisson = Redisson.create(config);
        return redisson;

    }
}
