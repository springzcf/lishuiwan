package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.BizException;
import com.lishuiwan.config.AppProperties;
import com.lishuiwan.domain.Member;
import com.lishuiwan.domain.MemberWechatIdentity;
import com.lishuiwan.mapper.MemberMapper;
import com.lishuiwan.mapper.MemberMergeMapper;
import com.lishuiwan.mapper.MemberWechatIdentityMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WechatIdentityService {
  public static final String MINI_PROGRAM="mini_program";
  public static final String OFFICIAL_ACCOUNT="official_account";

  private final MemberMapper members;
  private final MemberWechatIdentityMapper identities;
  private final MemberMergeMapper merge;
  private final AppProperties properties;

  public WechatIdentityService(MemberMapper members,MemberWechatIdentityMapper identities,MemberMergeMapper merge,AppProperties properties){
    this.members=members;this.identities=identities;this.merge=merge;this.properties=properties;
  }

  @Transactional
  public Member findMini(WechatClient.Identity source){
    String appId=miniAppId();
    MemberWechatIdentity exact=exact(MINI_PROGRAM,appId,source.openid());
    Member legacy=members.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getOpenid,source.openid()));
    Member union=byUnion(source.unionid());
    Member resolved=legacy!=null?legacy:exact==null?union:members.selectById(exact.getMemberId());
    if(resolved==null)return null;
    if(exact!=null)resolved=mergeInto(resolved,members.selectById(exact.getMemberId()));
    if(union!=null)resolved=mergeInto(resolved,union);
    bind(resolved.getId(),MINI_PROGRAM,appId,source);
    return resolved;
  }

  @Transactional
  public Member bindMini(Member member,WechatClient.Identity source){
    Member existing=findMini(source);
    Member resolved=existing==null?member:mergeInto(member,existing);
    bind(resolved.getId(),MINI_PROGRAM,miniAppId(),source);
    return resolved;
  }

  @Transactional
  public Member loginOfficial(WechatClient.Identity source){
    String appId=officialAppId();
    MemberWechatIdentity exact=exact(OFFICIAL_ACCOUNT,appId,source.openid());
    Member exactMember=exact==null?null:members.selectById(exact.getMemberId());
    Member union=byUnion(source.unionid());
    Member resolved=union!=null?union:exactMember;
    if(resolved==null){resolved=new Member();resolved.setOpenid("oa:"+source.openid());resolved.setNickname("微信会员");resolved.setStaffRole("customer");resolved.setLevel(0);resolved.setPoints(0);resolved.setStatus(0);members.insert(resolved);}
    if(exactMember!=null)resolved=mergeInto(resolved,exactMember);
    bind(resolved.getId(),OFFICIAL_ACCOUNT,appId,source);
    return resolved;
  }

  public String requireOpenid(long memberId,String provider){
    String openid=findOpenid(memberId,provider);
    if(openid!=null)return openid;
    throw new BizException(40906,OFFICIAL_ACCOUNT.equals(provider)?"服务号身份未绑定，请从服务号菜单重新进入":"小程序身份未绑定",HttpStatus.CONFLICT);
  }

  public String findOpenid(long memberId,String provider){
    String appId=OFFICIAL_ACCOUNT.equals(provider)?officialAppId():miniAppId();
    List<MemberWechatIdentity> rows=identities.selectList(new LambdaQueryWrapper<MemberWechatIdentity>().eq(MemberWechatIdentity::getMemberId,memberId).eq(MemberWechatIdentity::getProvider,provider).eq(MemberWechatIdentity::getAppId,appId).orderByDesc(MemberWechatIdentity::getId).last("limit 1"));
    if(!rows.isEmpty())return rows.get(0).getOpenid();
    if(MINI_PROGRAM.equals(provider)){Member legacy=members.selectById(memberId);if(legacy!=null&&!legacy.getOpenid().startsWith("oa:"))return legacy.getOpenid();}
    return null;
  }

  private MemberWechatIdentity exact(String provider,String appId,String openid){return identities.selectOne(new LambdaQueryWrapper<MemberWechatIdentity>().eq(MemberWechatIdentity::getProvider,provider).eq(MemberWechatIdentity::getAppId,appId).eq(MemberWechatIdentity::getOpenid,openid));}

  private Member byUnion(String unionid){
    if(unionid==null||unionid.isBlank())return null;
    List<MemberWechatIdentity> rows=identities.selectList(new LambdaQueryWrapper<MemberWechatIdentity>().eq(MemberWechatIdentity::getUnionid,unionid).orderByAsc(MemberWechatIdentity::getId).last("limit 1"));
    return rows.isEmpty()?null:members.selectById(rows.get(0).getMemberId());
  }

  private void bind(long memberId,String provider,String appId,WechatClient.Identity source){
    MemberWechatIdentity row=exact(provider,appId,source.openid());
    if(row==null){row=new MemberWechatIdentity();row.setMemberId(memberId);row.setProvider(provider);row.setAppId(appId);row.setOpenid(source.openid());row.setUnionid(source.unionid());identities.insert(row);return;}
    boolean changed=!row.getMemberId().equals(memberId)||source.unionid()!=null&&!source.unionid().equals(row.getUnionid());
    if(changed){row.setMemberId(memberId);if(source.unionid()!=null)row.setUnionid(source.unionid());identities.updateById(row);}
  }

  private Member mergeInto(Member winner,Member loser){
    if(loser==null||winner.getId().equals(loser.getId()))return winner;
    if(winner.getPhone()!=null&&loser.getPhone()!=null&&!winner.getPhone().equals(loser.getPhone()))throw new BizException(40905,"微信身份关联到不同手机号，请联系门店合并会员",HttpStatus.CONFLICT);
    merge.duplicateIdempotency(winner.getId(),loser.getId());
    merge.orders(winner.getId(),loser.getId());merge.orderOperators(winner.getId(),loser.getId());merge.cards(winner.getId(),loser.getId());
    merge.verifications(winner.getId(),loser.getId());merge.verificationOperators(winner.getId(),loser.getId());merge.notifications(winner.getId(),loser.getId());
    merge.consumedCodes(winner.getId(),loser.getId());merge.idempotency(winner.getId(),loser.getId());merge.identities(winner.getId(),loser.getId());
    if(winner.getPhone()==null&&loser.getPhone()!=null){String phone=loser.getPhone();loser.setPhone(null);members.updateById(loser);winner.setPhone(phone);}
    mergeProfile(winner,loser);members.updateById(winner);members.deleteById(loser.getId());return winner;
  }

  private void mergeProfile(Member winner,Member loser){
    if(blank(winner.getNickname())||"微信会员".equals(winner.getNickname()))winner.setNickname(loser.getNickname());
    if(blank(winner.getAvatar()))winner.setAvatar(loser.getAvatar());if(blank(winner.getBirthday()))winner.setBirthday(loser.getBirthday());if(blank(winner.getAddress()))winner.setAddress(loser.getAddress());
    winner.setLevel(Math.max(value(winner.getLevel()),value(loser.getLevel())));winner.setPoints(value(winner.getPoints())+value(loser.getPoints()));winner.setStatus(Math.max(value(winner.getStatus()),value(loser.getStatus())));
    if(roleRank(loser.getStaffRole())>roleRank(winner.getStaffRole())){winner.setStaffRole(loser.getStaffRole());winner.setStaffPin(loser.getStaffPin());}
    if(winner.getCreatedAt()==null||loser.getCreatedAt()!=null&&loser.getCreatedAt().isBefore(winner.getCreatedAt()))winner.setCreatedAt(loser.getCreatedAt());
  }

  private int roleRank(String role){return "admin".equals(role)?2:"verifier".equals(role)?1:0;}
  private int value(Integer value){return value==null?0:value;}
  private boolean blank(String value){return value==null||value.isBlank();}
  private String miniAppId(){String value=properties.getWechat().getAppId();return blank(value)?"dev-mini-program":value;}
  private String officialAppId(){String value=properties.getWechat().getOfficialAccount().getAppId();if(blank(value))throw new BizException(50002,"服务号 AppID 未配置");return value;}
}
