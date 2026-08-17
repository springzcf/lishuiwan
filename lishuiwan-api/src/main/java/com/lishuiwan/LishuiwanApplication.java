package com.lishuiwan;

import com.lishuiwan.config.AppProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.lishuiwan.mapper")
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class LishuiwanApplication {
  public static void main(String[] args) { SpringApplication.run(LishuiwanApplication.class, args); }
}
