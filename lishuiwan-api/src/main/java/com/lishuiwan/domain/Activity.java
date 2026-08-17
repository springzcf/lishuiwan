package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_activity") public class Activity {
  @TableId(type=IdType.AUTO) private Long id; private String title; private String image; private String targetType; private Long targetId;
  private LocalDateTime startAt; private LocalDateTime endAt; private Integer sort; private Integer status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
