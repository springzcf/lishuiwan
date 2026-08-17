package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("t_card_product") public class CardProduct {
  @TableId(type=IdType.AUTO) private Long id; private String name; private String category; private String cover; private String benefits;
  private BigDecimal price; private BigDecimal salePrice; private BigDecimal activityPrice; private LocalDateTime activityStartAt; private LocalDateTime activityEndAt;
  private Integer validDays; private String rules; private Integer status; private Integer sort; @TableLogic private Integer deleted;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
