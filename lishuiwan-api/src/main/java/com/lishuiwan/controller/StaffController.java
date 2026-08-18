package com.lishuiwan.controller;

import com.lishuiwan.common.*; import com.lishuiwan.domain.Member; import com.lishuiwan.service.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*; import java.math.BigDecimal; import java.util.*;

@RestController @RequestMapping("/api/wx/staff")
public class StaffController {
  private final StaffService staff; private final VerificationService verification; private final CashIssueService cash; private final AdminService admin; private final AuthService auth;
  public StaffController(StaffService staff,VerificationService verification,CashIssueService cash,AdminService admin,AuthService auth){this.staff=staff;this.verification=verification;this.cash=cash;this.admin=admin;this.auth=auth;}
  public record ParseRequest(@NotBlank String token){}
  public record VerifyRequest(@NotBlank String requestNo,@NotBlank String cardCodeToken,@NotNull Long cardId,@NotBlank String benefitId,@NotNull @DecimalMin("0.01") BigDecimal quantity){}
  public record CashRequest(@NotBlank String requestNo,@NotBlank String memberCodeToken,@NotNull Long productId,@NotNull @DecimalMin("0.01") BigDecimal paidAmount,@NotBlank @Size(max=32) @Pattern(regexp="\\d{4,6}") String staffPin){}
  @PostMapping("/member-codes/parse") public ApiResponse<Map<String,Object>> parseMember(@Valid @RequestBody ParseRequest r){staff.require(RequestActor.memberId(),true);return ApiResponse.ok(cash.parseMember(r.token()));}
  @PostMapping("/card-codes/parse") public ApiResponse<Map<String,Object>> parseCard(@Valid @RequestBody ParseRequest r){staff.require(RequestActor.memberId(),false);return ApiResponse.ok(verification.parse(r.token()));}
  @PostMapping("/verifications") public ApiResponse<Map<String,Object>> verify(@Valid @RequestBody VerifyRequest r){long id=RequestActor.memberId();Member operator=staff.require(id,false);return ApiResponse.ok(verification.verify(operator,r.requestNo(),r.cardCodeToken(),r.cardId(),r.benefitId(),r.quantity()));}
  @PostMapping("/cash-orders") public ApiResponse<Map<String,Object>> cash(@Valid @RequestBody CashRequest r){long id=RequestActor.memberId();staff.require(id,true);return ApiResponse.ok(cash.issue(id,r.requestNo(),r.memberCodeToken(),r.productId(),r.paidAmount(),r.staffPin()));}
  @GetMapping("/pin") public ApiResponse<Map<String,Object>> pinStatus(){long id=RequestActor.memberId();staff.require(id,true);return ApiResponse.ok(auth.pinStatus(id));}
  @GetMapping("/verifications") public ApiResponse<List<Map<String,Object>>> records(){staff.require(RequestActor.memberId(),false);return ApiResponse.ok(verification.records());}
  @GetMapping("/reports/summary") public ApiResponse<Map<String,Object>> summary(){staff.require(RequestActor.memberId(),true);return ApiResponse.ok(admin.overview());}
}
