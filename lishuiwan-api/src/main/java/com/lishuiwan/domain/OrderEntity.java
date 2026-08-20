package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("t_order") public class OrderEntity {
  @TableId(type=IdType.AUTO) private Long id; private String orderNo; private Long memberId; private BigDecimal payableAmount; private BigDecimal paidAmount;
  private String payMethod; private Integer payStatus; private Long operatorMemberId; private String transactionId; private String paymentAppid; private LocalDateTime paidAt;
  private LocalDateTime expireAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
