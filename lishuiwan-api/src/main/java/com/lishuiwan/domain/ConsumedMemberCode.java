package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_consumed_member_code") public class ConsumedMemberCode {
  @TableId(type=IdType.INPUT) private String nonce; private Long memberId; private String action; private LocalDateTime createdAt; private LocalDateTime expireAt;
}
