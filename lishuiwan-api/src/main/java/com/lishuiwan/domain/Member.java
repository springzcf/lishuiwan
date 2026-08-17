package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_member") public class Member {
  @TableId(type=IdType.AUTO) private Long id; private String openid; private String phone; private String nickname; private String avatar;
  private String staffRole; private Integer level; private Integer points; private Integer status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
