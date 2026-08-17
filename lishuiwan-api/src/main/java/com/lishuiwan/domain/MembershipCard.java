package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_membership_card") public class MembershipCard {
  @TableId(type=IdType.AUTO) private Long id; private Long memberId; private Long productId; private Long orderItemId; private String benefitsRemaining;
  private LocalDateTime validFrom; private LocalDateTime validUntil; private String status; @Version private Integer version;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
