package com.lishuiwan.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BizException extends RuntimeException {
  private final int code;
  private final HttpStatus status;
  public BizException(int code, String message) { this(code, message, HttpStatus.UNPROCESSABLE_ENTITY); }
  public BizException(int code, String message, HttpStatus status) { super(message); this.code = code; this.status = status; }
  public static BizException unauthorized() { return new BizException(40001, "登录已失效", HttpStatus.UNAUTHORIZED); }
  public static BizException forbidden() { return new BizException(40301, "无权执行此操作", HttpStatus.FORBIDDEN); }
  public static BizException notFound(String target) { return new BizException(40401, target + "不存在", HttpStatus.NOT_FOUND); }
}
