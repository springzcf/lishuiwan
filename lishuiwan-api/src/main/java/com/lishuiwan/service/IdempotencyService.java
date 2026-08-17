package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lishuiwan.common.BizException;
import com.lishuiwan.common.Ids;
import com.lishuiwan.domain.IdempotencyRecord;
import com.lishuiwan.mapper.IdempotencyMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
  private final IdempotencyMapper mapper; private final ObjectMapper json;
  public IdempotencyService(IdempotencyMapper mapper, ObjectMapper json) { this.mapper=mapper; this.json=json; }

  @Transactional
  public Map<String,Object> execute(String actorType, long actorId, String scope, String requestNo, Object request, Supplier<Map<String,Object>> action) {
    if (requestNo == null || !requestNo.matches("[A-Za-z0-9_-]{8,64}")) throw new BizException(40002,"requestNo 格式错误",HttpStatus.BAD_REQUEST);
    String requestHash = Ids.sha256(write(request));
    IdempotencyRecord fresh = new IdempotencyRecord(); fresh.setActorType(actorType); fresh.setActorId(actorId); fresh.setApiScope(scope);
    fresh.setRequestNo(requestNo); fresh.setRequestHash(requestHash); fresh.setStatus("processing"); fresh.setExpireAt(LocalDateTime.now().plusDays(1));
    if (mapper.insertIgnore(fresh) == 0) {
      IdempotencyRecord old = mapper.selectOne(new LambdaQueryWrapper<IdempotencyRecord>().eq(IdempotencyRecord::getActorType,actorType)
        .eq(IdempotencyRecord::getActorId,actorId).eq(IdempotencyRecord::getApiScope,scope).eq(IdempotencyRecord::getRequestNo,requestNo));
      if (old == null || !requestHash.equals(old.getRequestHash())) throw new BizException(40901,"requestNo 已被不同请求使用",HttpStatus.CONFLICT);
      if (!"succeeded".equals(old.getStatus()) || old.getResponseSnapshot() == null) throw new BizException(40901,"请求正在处理中",HttpStatus.CONFLICT);
      return read(old.getResponseSnapshot());
    }
    Map<String,Object> result = action.get();
    fresh.setStatus("succeeded"); fresh.setResponseSnapshot(write(result)); mapper.updateById(fresh);
    return result;
  }
  private String write(Object v) { try { return json.writeValueAsString(v); } catch(Exception e) { throw new IllegalStateException(e); } }
  private Map<String,Object> read(String v) { try { return json.readValue(v,new TypeReference<>(){}); } catch(Exception e) { throw new IllegalStateException(e); } }
}
