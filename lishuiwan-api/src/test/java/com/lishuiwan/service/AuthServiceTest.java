package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import com.lishuiwan.domain.AdminUser;
import com.lishuiwan.mapper.AdminUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {
  @Test void acceptsPasswordFromAnyActiveAdministrator(){
    AdminUserMapper admins=mock(AdminUserMapper.class);
    BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    AdminUser active=admin(1L,0,passwords.encode("correct-password"));
    when(admins.selectList(any())).thenReturn(List.of(active));
    AuthService service=new AuthService(null,null,admins,null,passwords);

    assertThatCode(()->service.requireActiveAdminPassword("correct-password")).doesNotThrowAnyException();
  }

  @Test void rejectsWrongAdministratorPassword(){
    AdminUserMapper admins=mock(AdminUserMapper.class);
    BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    when(admins.selectList(any())).thenReturn(List.of(admin(1L,0,passwords.encode("correct-password"))));
    AuthService service=new AuthService(null,null,admins,null,passwords);

    assertThatThrownBy(()->service.requireActiveAdminPassword("wrong-password"))
        .isInstanceOf(BizException.class).hasMessageContaining("管理员密码错误");
  }

  private AdminUser admin(long id,int status,String password){
    AdminUser admin=new AdminUser();admin.setId(id);admin.setStatus(status);admin.setPassword(password);return admin;
  }
}
