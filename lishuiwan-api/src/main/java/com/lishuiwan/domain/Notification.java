package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_notification") public class Notification {
  @TableId(type=IdType.AUTO) private Long id; private Long memberId; private String type; private String title; private String content; private Long refId;
  private Integer isRead; private String pushStatus; private Integer pushAttempts; private String pushLastError; private LocalDateTime createdAt;
}
