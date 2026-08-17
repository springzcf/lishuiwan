package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("t_order_item") public class OrderItem {
  @TableId(type=IdType.AUTO) private Long id; private Long orderId; private Long productId; private Integer quantity; private BigDecimal unitPrice;
  private String productSnapshot; private LocalDateTime createdAt;
}
