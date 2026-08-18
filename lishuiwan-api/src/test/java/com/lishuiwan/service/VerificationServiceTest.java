package com.lishuiwan.service;

import com.lishuiwan.domain.Verification;
import com.lishuiwan.mapper.MemberMapper;
import com.lishuiwan.mapper.MembershipCardMapper;
import com.lishuiwan.mapper.VerificationMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificationServiceTest {
  @Test void recordsExposeTheContextCapturedAtVerificationTime(){
    VerificationMapper verifications=mock(VerificationMapper.class);
    MemberMapper members=mock(MemberMapper.class);
    MembershipCardMapper cards=mock(MembershipCardMapper.class);
    Verification record=new Verification();record.setId(10L);record.setMemberId(2L);record.setCardId(3L);record.setOperatorMemberId(4L);record.setItemSnapshot("成人洗浴");record.setQuantity(new BigDecimal("2"));record.setMemberNameSnapshot("张三");record.setMemberPhoneSnapshot("13800138000");record.setProductNameSnapshot("VIP 次卡");record.setOperatorName("前台一号");record.setOperatorPhoneSnapshot("13900139000");record.setCreatedAt(LocalDateTime.now());
    when(verifications.selectList(any())).thenReturn(List.of(record));
    when(members.selectByIds(any())).thenReturn(List.of());
    when(cards.selectByIds(any())).thenReturn(List.of());
    VerificationService service=new VerificationService(null,members,cards,verifications,null,null,null,null,null);

    Map<String,Object> result=service.records().get(0);

    assertThat(result).containsEntry("memberName","张三").containsEntry("memberPhone","13800138000")
        .containsEntry("productName","VIP 次卡").containsEntry("operatorName","前台一号")
        .containsEntry("operatorPhone","13900139000");
  }
}
