package com.lishuiwan.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class Jsons {
  private final ObjectMapper mapper;
  public Jsons(ObjectMapper mapper) { this.mapper = mapper; }
  public String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException(e); } }
  public <T> T read(String value, Class<T> type) { try { return mapper.readValue(value, type); } catch (Exception e) { throw new IllegalStateException(e); } }
  public List<Map<String,Object>> benefits(String value) { try { return mapper.readValue(value, new TypeReference<>() {}); } catch (Exception e) { throw new IllegalStateException(e); } }
}
