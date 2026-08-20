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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {
    private final WechatClient wechat;
    private final MemberMapper members;
    private final AdminUserMapper admins;
    private final TokenService tokens;
    private final PasswordEncoder passwords;
    private final WechatIdentityService identities;

    public AuthService(WechatClient wechat, MemberMapper members, AdminUserMapper admins, TokenService tokens, PasswordEncoder passwords,WechatIdentityService identities) {
        this.wechat = wechat;
        this.members = members;
        this.admins = admins;
        this.tokens = tokens;
        this.passwords = passwords;
        this.identities = identities;
    }

    public Map<String, Object> wxLogin(String code) {
        WechatClient.Identity source=wechat.miniIdentity(code);
        Member m=identities.findMini(source);
        if (m == null || m.getPhone() == null)
            return Map.of("registered", false, "registrationToken", tokens.registration(source.openid(),source.unionid()));
        ensureActive(m);
        return loginResult(m,true);
    }

    @Transactional
    public Map<String, Object> bindPhone(String registrationToken, String phoneCode) {
        Claims claims = tokens.parse(registrationToken);
        if (!"registration".equals(claims.get("type", String.class))) throw BizException.unauthorized();
        WechatClient.Identity source=new WechatClient.Identity(claims.getSubject(),claims.get("unionid",String.class));
        String phone = wechat.phone(phoneCode);
        Member byOpenid = identities.findMini(source);
        Member byPhone = members.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getPhone, phone));
        Member m;
        if (byOpenid != null) {
            if (byOpenid.getPhone() != null && !phone.equals(byOpenid.getPhone()))
                throw new BizException(40904, "微信账号已绑定其他手机号");
            if(byPhone!=null&&!byPhone.getId().equals(byOpenid.getId()))m=identities.bindMini(byPhone,source);
            else{if(byOpenid.getPhone()==null){byOpenid.setPhone(phone);members.updateById(byOpenid);}m=identities.bindMini(byOpenid,source);}
        } else if (byPhone != null) {
            m=identities.bindMini(byPhone,source);
        } else {
            m = new Member();
            m.setOpenid(source.openid());
            m.setPhone(phone);
            m.setNickname("微信会员");
            m.setStaffRole("customer");
            m.setLevel(0);
            m.setPoints(0);
            m.setStatus(0);
            members.insert(m);
            m=identities.bindMini(m,source);
        }
        ensureActive(m);
        return loginResult(m,false);
    }

    public long officialMemberId(WechatClient.Identity source){Member member=identities.loginOfficial(source);ensureActive(member);return member.getId();}

    public Map<String,Object> memberLogin(long memberId){Member member=members.selectById(memberId);if(member==null)throw BizException.unauthorized();ensureActive(member);return loginResult(member,false);}

    public Map<String, Object> adminLogin(String username, String password) {
        AdminUser a = admins.selectOne(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (a == null || a.getStatus() != 0 || !passwords.matches(password, a.getPassword()))
            throw new BizException(40005, "账号或密码错误");
        return Map.of("token", tokens.admin(a.getId()), "admin", Map.of("id", a.getId(), "username", a.getUsername(), "name", a.getName()));
    }

    public void requireStaffPin(long memberId, String pin) {
        Member m = members.selectById(memberId);
        if (m == null || m.getStatus() != 0) throw BizException.unauthorized();
        if (m.getStaffPin() == null) throw new BizException(40303, "未设置操作密码", HttpStatus.FORBIDDEN);
        if (!passwords.matches(pin, m.getStaffPin())) throw new BizException(40005, "操作密码错误");
    }

    public Map<String, Object> pinStatus(long memberId) {
        Member m = members.selectById(memberId);
        if (m == null || m.getStatus() != 0) throw BizException.unauthorized();
        return Map.of("hasPin", m.getStaffPin() != null);
    }

    private void ensureActive(Member m) {
        if (m.getStatus() != 0) throw new BizException(40302, "会员账号已禁用");
    }

    private Map<String,Object> loginResult(Member member,boolean registered){return registered?Map.of("registered",true,"token",tokens.member(member.getId()),"member",safe(member)):Map.of("token",tokens.member(member.getId()),"member",safe(member));}

    public Map<String, Object> safe(Member m) {
        return Map.of("id", m.getId(), "phone", mask(m.getPhone()), "nickname", m.getNickname() == null ? "" : m.getNickname(), "avatar", m.getAvatar() == null ? "" : m.getAvatar(), "birthday", m.getBirthday() == null ? "" : m.getBirthday(), "address", m.getAddress() == null ? "" : m.getAddress(), "staffRole", m.getStaffRole(), "createdAt", m.getCreatedAt() == null ? "" : m.getCreatedAt().toString());
    }

    private String mask(String p) {
        return p == null ? "" : p.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }
}
