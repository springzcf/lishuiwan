package com.lishuiwan.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("t_operation_log") public class OperationLog {
  @TableId(type=IdType.AUTO) private Long id; private String operatorType; private Long operatorId; private String action; private String targetType;
  private Long targetId; private String detail; private LocalDateTime createdAt;
}
