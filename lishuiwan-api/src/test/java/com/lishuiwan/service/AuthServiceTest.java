package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import com.lishuiwan.domain.Member;
import com.lishuiwan.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
  @Test void requireStaffPinAcceptsMatchingPin(){
    MemberMapper members=mock(MemberMapper.class);
    BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    Member m=member(1L,0,passwords.encode("123456"));
    when(members.selectById(1L)).thenReturn(m);
    AuthService service=new AuthService(null,members,null,null,passwords);

    assertThatCode(()->service.requireStaffPin(1L,"123456")).doesNotThrowAnyException();
  }

  @Test void requireStaffPinRejectsWrongPin(){
    MemberMapper members=mock(MemberMapper.class);
    BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    when(members.selectById(1L)).thenReturn(member(1L,0,passwords.encode("123456")));
    AuthService service=new AuthService(null,members,null,null,passwords);

    assertThatThrownBy(()->service.requireStaffPin(1L,"000000"))
        .isInstanceOf(BizException.class).hasMessageContaining("操作密码错误");
  }

  @Test void requireStaffPinFailsWhenNotSet(){
    MemberMapper members=mock(MemberMapper.class);
    BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(4);
    when(members.selectById(1L)).thenReturn(member(1L,0,null));
    AuthService service=new AuthService(null,members,null,null,passwords);

    assertThatThrownBy(()->service.requireStaffPin(1L,"123456"))
        .isInstanceOf(BizException.class).hasMessageContaining("未设置操作密码");
  }

  @SuppressWarnings("unchecked")
  private Member member(long id,int status,String staffPin){
    Member m=new Member();m.setId(id);m.setStatus(status);m.setStaffPin(staffPin);return m;
  }
}
