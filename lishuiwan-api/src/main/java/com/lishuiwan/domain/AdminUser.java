package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_admin_user") public class AdminUser {
  @TableId(type=IdType.AUTO) private Long id; private String username; private String password; private String name; private Integer status;
  private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
