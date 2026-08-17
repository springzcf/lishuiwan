package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("t_verification") public class Verification {
  @TableId(type=IdType.AUTO) private Long id; private String requestNo; private Long cardId; private Long memberId; private String benefitId;
  private String itemSnapshot; private BigDecimal quantity; private String operatorName; private Long operatorMemberId; private LocalDateTime createdAt;
}
