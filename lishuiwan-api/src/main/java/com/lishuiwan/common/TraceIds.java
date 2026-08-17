package com.lishuiwan.common;

import org.slf4j.MDC;
import java.util.UUID;

public final class TraceIds {
  private TraceIds() {}
  public static String current() {
    String id = MDC.get("traceId");
    return id == null ? "" : id;
  }
  public static String create() { return UUID.randomUUID().toString().replace("-", ""); }
}
