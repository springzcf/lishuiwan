package com.lishuiwan.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

public interface MemberMergeMapper {
  @Delete("DELETE loser FROM t_idempotency_record loser JOIN t_idempotency_record winner ON winner.actor_type='member' AND winner.actor_id=#{winner} AND winner.api_scope=loser.api_scope AND winner.request_no=loser.request_no WHERE loser.actor_type='member' AND loser.actor_id=#{loser}")
  int duplicateIdempotency(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_order SET member_id=#{winner} WHERE member_id=#{loser}")
  int orders(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_order SET operator_member_id=#{winner} WHERE operator_member_id=#{loser}")
  int orderOperators(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_membership_card SET member_id=#{winner} WHERE member_id=#{loser}")
  int cards(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_verification SET member_id=#{winner} WHERE member_id=#{loser}")
  int verifications(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_verification SET operator_member_id=#{winner} WHERE operator_member_id=#{loser}")
  int verificationOperators(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_notification SET member_id=#{winner} WHERE member_id=#{loser}")
  int notifications(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_consumed_member_code SET member_id=#{winner} WHERE member_id=#{loser}")
  int consumedCodes(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_idempotency_record SET actor_id=#{winner} WHERE actor_type='member' AND actor_id=#{loser}")
  int idempotency(@Param("winner") long winner,@Param("loser") long loser);

  @Update("UPDATE t_member_wechat_identity SET member_id=#{winner} WHERE member_id=#{loser}")
  int identities(@Param("winner") long winner,@Param("loser") long loser);
}
