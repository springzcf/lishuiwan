package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import com.lishuiwan.domain.Member;
import com.lishuiwan.mapper.MemberMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal; import java.util.Map;

@Service
public class CashIssueService {
  private final MemberCodeService codes; private final OrderService orders; private final IdempotencyService idem; private final MemberMapper members; private final AuthService auth;
  public CashIssueService(MemberCodeService codes,OrderService orders,IdempotencyService idem,MemberMapper members,AuthService auth){this.codes=codes;this.orders=orders;this.idem=idem;this.members=members;this.auth=auth;}
  public Map<String,Object> parseMember(String token){MemberCodeService.CodeData code=codes.validate(token);Member member=requireActive(code.memberId());return Map.of("member",auth.safe(member));}
  public Map<String,Object> issue(long operatorId,String requestNo,String token,long productId,BigDecimal paidAmount,String adminPassword){auth.requireActiveAdminPassword(adminPassword);return idem.execute("member",operatorId,"cash_issue",requestNo,Map.of("memberCodeToken",token,"productId",productId,"paidAmount",paidAmount),()->{MemberCodeService.CodeData code=codes.validate(token);Member member=requireActive(code.memberId());return orders.cashOnce(operatorId,member.getId(),productId,paidAmount);});}
  private Member requireActive(long memberId){Member member=members.selectById(memberId);if(member==null)throw BizException.notFound("会员");if(member.getStatus()!=0)throw new BizException(40301,"会员账号不可用");return member;}
}
