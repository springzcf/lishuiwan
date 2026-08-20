package com.lishuiwan.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_member_wechat_identity")
public class MemberWechatIdentity {
  @TableId(type = IdType.AUTO) private Long id;
  private Long memberId;
  private String provider;
  private String appId;
  private String openid;
  private String unionid;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
