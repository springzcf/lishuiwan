package com.lishuiwan.controller;

import com.lishuiwan.common.ApiResponse; import com.lishuiwan.service.AuthService; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*; import org.springframework.http.ResponseEntity; import java.util.Map;

@RestController @RequestMapping("/api/public")
public class PublicController {
  private final AuthService auth; private final com.lishuiwan.service.WechatPayService pay; public PublicController(AuthService auth,com.lishuiwan.service.WechatPayService pay){this.auth=auth;this.pay=pay;}
  public record WxLoginRequest(@NotBlank String loginCode){}
  public record PhoneRequest(@NotBlank String registrationToken,@NotBlank String phoneCode){}
  public record AdminLoginRequest(@NotBlank String username,@NotBlank String password){}
  @PostMapping("/wx/login") public ApiResponse<Map<String,Object>> login(@Valid @RequestBody WxLoginRequest r){return ApiResponse.ok(auth.wxLogin(r.loginCode()));}
  @PostMapping("/wx/phone") public ApiResponse<Map<String,Object>> phone(@Valid @RequestBody PhoneRequest r){return ApiResponse.ok(auth.bindPhone(r.registrationToken(),r.phoneCode()));}
  @PostMapping("/admin/login") public ApiResponse<Map<String,Object>> admin(@Valid @RequestBody AdminLoginRequest r){return ApiResponse.ok(auth.adminLogin(r.username(),r.password()));}
  @PostMapping("/wx/pay/notify") public ResponseEntity<Map<String,String>> payNotify(@RequestHeader("Wechatpay-Serial") String serial,@RequestHeader("Wechatpay-Signature") String signature,@RequestHeader("Wechatpay-Timestamp") String timestamp,@RequestHeader("Wechatpay-Nonce") String nonce,@RequestBody String body){pay.notify(serial,signature,timestamp,nonce,body);return ResponseEntity.ok(Map.of("code","SUCCESS","message","成功"));}
}
