package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal; import java.util.Map;

@Service
public class CashIssueService {
  private final MemberCodeService codes; private final OrderService orders; private final IdempotencyService idem;
  public CashIssueService(MemberCodeService codes,OrderService orders,IdempotencyService idem){this.codes=codes;this.orders=orders;this.idem=idem;}
  public Map<String,Object> issue(long operatorId,String requestNo,String token,long productId,BigDecimal paidAmount){return idem.execute("member",operatorId,"cash_issue",requestNo,Map.of("memberCodeToken",token,"productId",productId,"paidAmount",paidAmount),()->{MemberCodeService.CodeData code=codes.validate(token);Map<String,Object> result=orders.cashOnce(operatorId,code.memberId(),productId,paidAmount);codes.consume(code,"cash_issue");codes.clear(code);return result;});}
}
