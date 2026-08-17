package com.lishuiwan.common;

public record ApiResponse<T>(int code, String msg, T data, String traceId) {
  public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "ok", data, TraceIds.current()); }
}
