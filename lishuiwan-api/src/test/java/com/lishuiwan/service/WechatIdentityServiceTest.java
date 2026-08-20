package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lishuiwan.config.AppProperties;
import com.lishuiwan.domain.Member;
import com.lishuiwan.domain.MemberWechatIdentity;
import com.lishuiwan.mapper.MemberMapper;
import com.lishuiwan.mapper.MemberMergeMapper;
import com.lishuiwan.mapper.MemberWechatIdentityMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WechatIdentityServiceTest {
  @Test
  @SuppressWarnings("unchecked")
  void officialLoginCreatesMemberAndIdentity(){
    MemberMapper members=mock(MemberMapper.class);MemberWechatIdentityMapper identities=mock(MemberWechatIdentityMapper.class);MemberMergeMapper merge=mock(MemberMergeMapper.class);
    when(identities.selectOne(any(Wrapper.class))).thenReturn(null);when(identities.selectList(any(Wrapper.class))).thenReturn(List.of());
    doAnswer(call->{Member member=call.getArgument(0);member.setId(12L);return 1;}).when(members).insert(any(Member.class));
    doAnswer(call->{MemberWechatIdentity identity=call.getArgument(0);identity.setId(3L);return 1;}).when(identities).insert(any(MemberWechatIdentity.class));
    AppProperties properties=new AppProperties();properties.getWechat().getOfficialAccount().setAppId("wx-official");
    WechatIdentityService service=new WechatIdentityService(members,identities,merge,properties);

    Member member=service.loginOfficial(new WechatClient.Identity("openid-1","union-1"));

    assertThat(member.getId()).isEqualTo(12L);assertThat(member.getOpenid()).isEqualTo("oa:openid-1");
    verify(identities).insert(argThat((MemberWechatIdentity row)->row.getMemberId()==12L&&"official_account".equals(row.getProvider())&&"union-1".equals(row.getUnionid())));
  }
}
