package com.lishuiwan.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BizException.class)
  ResponseEntity<ApiResponse<Void>> biz(BizException e) {
    return ResponseEntity.status(e.getStatus()).body(new ApiResponse<>(e.getCode(), e.getMessage(), null, TraceIds.current()));
  }
  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  ResponseEntity<ApiResponse<Void>> validation(Exception e) {
    String msg = e instanceof MethodArgumentNotValidException m
      ? m.getBindingResult().getFieldErrors().stream().map(x -> x.getField() + x.getDefaultMessage()).collect(Collectors.joining(";"))
      : e.getMessage();
    return ResponseEntity.badRequest().body(new ApiResponse<>(40002, msg, null, TraceIds.current()));
  }
  @ExceptionHandler(DuplicateKeyException.class)
  ResponseEntity<ApiResponse<Void>> duplicate(DuplicateKeyException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(40901, "请求已处理或数据已存在", null, TraceIds.current()));
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> system(Exception e) {
    log.error("Unhandled error traceId={}", TraceIds.current(), e);
    return ResponseEntity.status(500).body(new ApiResponse<>(50000, "系统繁忙，请稍后重试", null, TraceIds.current()));
  }
}
