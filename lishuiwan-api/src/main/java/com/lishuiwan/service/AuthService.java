package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.BizException;
import com.lishuiwan.domain.AdminUser;
import com.lishuiwan.domain.Member;
import com.lishuiwan.mapper.AdminUserMapper;
import com.lishuiwan.mapper.MemberMapper;
import com.lishuiwan.security.TokenService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class AuthService {
  private final WechatClient wechat; private final MemberMapper members; private final AdminUserMapper admins; private final TokenService tokens; private final PasswordEncoder passwords;
  public AuthService(WechatClient wechat,MemberMapper members,AdminUserMapper admins,TokenService tokens,PasswordEncoder passwords){this.wechat=wechat;this.members=members;this.admins=admins;this.tokens=tokens;this.passwords=passwords;}
  public Map<String,Object> wxLogin(String code){
    String openid=wechat.openid(code); Member m=members.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getOpenid,openid));
    if(m==null||m.getPhone()==null)return Map.of("registered",false,"registrationToken",tokens.registration(openid));
    ensureActive(m); return Map.of("registered",true,"token",tokens.member(m.getId()),"member",safe(m));
  }
  @Transactional public Map<String,Object> bindPhone(String registrationToken,String phoneCode){
    Claims claims=tokens.parse(registrationToken); if(!"registration".equals(claims.get("type",String.class)))throw BizException.unauthorized();
    String openid=claims.getSubject(), phone=wechat.phone(phoneCode);
    Member byOpenid=members.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getOpenid,openid));
    Member byPhone=members.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getPhone,phone));
    Member m;
    if(byOpenid!=null){ if(byOpenid.getPhone()!=null&&!phone.equals(byOpenid.getPhone()))throw new BizException(40904,"微信账号已绑定其他手机号"); byOpenid.setPhone(phone);members.updateById(byOpenid);m=byOpenid; }
    else if(byPhone!=null){ if(!openid.equals(byPhone.getOpenid()))throw new BizException(40904,"手机号已绑定其他微信账号");m=byPhone; }
    else { m=new Member();m.setOpenid(openid);m.setPhone(phone);m.setNickname("微信会员");m.setStaffRole("customer");m.setLevel(0);m.setPoints(0);m.setStatus(0);members.insert(m); }
    return Map.of("token",tokens.member(m.getId()),"member",safe(m));
  }
  public Map<String,Object> adminLogin(String username,String password){
    AdminUser a=admins.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername,username));
    if(a==null||a.getStatus()!=0||!passwords.matches(password,a.getPassword()))throw new BizException(40005,"账号或密码错误");
    return Map.of("token",tokens.admin(a.getId()),"admin",Map.of("id",a.getId(),"username",a.getUsername(),"name",a.getName()));
  }
  private void ensureActive(Member m){if(m.getStatus()!=0)throw new BizException(40302,"会员账号已禁用");}
  public Map<String,Object> safe(Member m){return Map.of("id",m.getId(),"phone",mask(m.getPhone()),"nickname",m.getNickname()==null?"":m.getNickname(),"avatar",m.getAvatar()==null?"":m.getAvatar(),"staffRole",m.getStaffRole(),"createdAt",m.getCreatedAt()==null?"":m.getCreatedAt().toString());}
  private String mask(String p){return p==null?"":p.replaceAll("(\\d{3})\\d{4}(\\d{4})","$1****$2");}
}
