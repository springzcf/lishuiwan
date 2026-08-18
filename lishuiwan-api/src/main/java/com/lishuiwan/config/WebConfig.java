package com.lishuiwan.config;

import com.lishuiwan.security.ApiAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import java.nio.file.Path;

@Configuration
public class WebConfig {
  @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
  @Bean RestClient.Builder restClientBuilder() { return RestClient.builder(); }
  @Bean WebMvcConfigurer uploadResourceConfigurer(AppProperties properties) {
    return new WebMvcConfigurer() {
      @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix=properties.getUpload().getUrlPrefix();
        if(!prefix.startsWith("/"))prefix="/"+prefix;
        if(!prefix.endsWith("/"))prefix=prefix+"/";
        String location=Path.of(properties.getUpload().getDirectory()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(prefix+"**").addResourceLocations(location).setCachePeriod(3600);
      }
    };
  }
  @Bean MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
  }
  @Bean FilterRegistrationBean<ApiAuthFilter> authFilterRegistration(ApiAuthFilter filter) {
    FilterRegistrationBean<ApiAuthFilter> bean = new FilterRegistrationBean<>(filter);
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE); bean.addUrlPatterns("/*"); return bean;
  }
}
