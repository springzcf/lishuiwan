package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_idempotency_record") public class IdempotencyRecord {
  @TableId(type=IdType.AUTO) private Long id; private String actorType; private Long actorId; private String apiScope; private String requestNo;
  private String requestHash; private String status; private String responseSnapshot; private LocalDateTime expireAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
