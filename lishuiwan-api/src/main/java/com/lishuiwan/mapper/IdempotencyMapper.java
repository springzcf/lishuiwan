package com.lishuiwan.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lishuiwan.domain.IdempotencyRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
public interface IdempotencyMapper extends BaseMapper<IdempotencyRecord> {
  @Insert("INSERT IGNORE INTO t_idempotency_record(actor_type,actor_id,api_scope,request_no,request_hash,status,expire_at) VALUES(#{actorType},#{actorId},#{apiScope},#{requestNo},#{requestHash},#{status},#{expireAt})")
  @Options(useGeneratedKeys=true,keyProperty="id")
  int insertIgnore(IdempotencyRecord record);
}
