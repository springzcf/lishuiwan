package com.lishuiwan.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.domain.AdminUser;
import com.lishuiwan.mapper.AdminUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
  private final AdminUserMapper mapper; private final PasswordEncoder encoder; private final Environment env;
  public AdminBootstrap(AdminUserMapper mapper,PasswordEncoder encoder,Environment env){this.mapper=mapper;this.encoder=encoder;this.env=env;}
  public void run(ApplicationArguments args){
    String user=env.getProperty("INITIAL_ADMIN_USERNAME"), pass=env.getProperty("INITIAL_ADMIN_PASSWORD"), name=env.getProperty("INITIAL_ADMIN_NAME","系统管理员");
    if(user==null||pass==null||pass.length()<12)return;
    if(mapper.selectCount(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername,user))==0){AdminUser a=new AdminUser();a.setUsername(user);a.setPassword(encoder.encode(pass));a.setName(name);a.setStatus(0);mapper.insert(a);}
  }
}
