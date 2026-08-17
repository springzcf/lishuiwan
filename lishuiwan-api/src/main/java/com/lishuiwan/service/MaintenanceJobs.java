package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*; import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Service; import java.time.LocalDateTime;

@Service
public class MaintenanceJobs {
  private final OrderMapper orders; private final MembershipCardMapper cards; private final IdempotencyMapper idempotency; private final ConsumedMemberCodeMapper codes;
  public MaintenanceJobs(OrderMapper orders,MembershipCardMapper cards,IdempotencyMapper idempotency,ConsumedMemberCodeMapper codes){this.orders=orders;this.cards=cards;this.idempotency=idempotency;this.codes=codes;}
  @Scheduled(fixedDelay=60000) public void closeOrders(){orders.update(null,new LambdaUpdateWrapper<OrderEntity>().eq(OrderEntity::getPayStatus,0).le(OrderEntity::getExpireAt,LocalDateTime.now()).set(OrderEntity::getPayStatus,2));}
  @Scheduled(fixedDelay=300000) public void expireCards(){cards.update(null,new LambdaUpdateWrapper<MembershipCard>().in(MembershipCard::getStatus,"unused","using").le(MembershipCard::getValidUntil,LocalDateTime.now()).set(MembershipCard::getStatus,"expired"));}
  @Scheduled(cron="0 20 3 * * *") public void cleanup(){idempotency.delete(new LambdaQueryWrapper<IdempotencyRecord>().lt(IdempotencyRecord::getExpireAt,LocalDateTime.now()));codes.delete(new LambdaQueryWrapper<ConsumedMemberCode>().lt(ConsumedMemberCode::getExpireAt,LocalDateTime.now()));}
}
