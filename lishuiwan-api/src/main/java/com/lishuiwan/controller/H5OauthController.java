package com.lishuiwan.controller;

import com.lishuiwan.common.ApiResponse;
import com.lishuiwan.service.H5OauthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/public/h5/wechat")
public class H5OauthController {
  private final H5OauthService oauth;
  public H5OauthController(H5OauthService oauth){this.oauth=oauth;}
  public record ExchangeRequest(@NotBlank String ticket){}

  @GetMapping("/authorize")
  public ResponseEntity<Void> authorize(@RequestParam(defaultValue="/h5/") String returnUrl){return redirect(oauth.authorize(returnUrl));}

  @GetMapping("/callback")
  public ResponseEntity<Void> callback(@RequestParam String code,@RequestParam String state){return redirect(oauth.callback(code,state));}

  @PostMapping("/exchange")
  public ResponseEntity<ApiResponse<Map<String,Object>>> exchange(@Valid @RequestBody ExchangeRequest request){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(oauth.exchange(request.ticket())));}

  private ResponseEntity<Void> redirect(String target){return ResponseEntity.status(302).header(HttpHeaders.CACHE_CONTROL,"no-store").location(URI.create(target)).build();}
}
