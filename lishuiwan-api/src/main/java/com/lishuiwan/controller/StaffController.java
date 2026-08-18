package com.lishuiwan.controller;

import com.lishuiwan.common.*; import com.lishuiwan.domain.Verification; import com.lishuiwan.service.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*; import java.math.BigDecimal; import java.util.*;

@RestController @RequestMapping("/api/wx/staff")
public class StaffController {
  private final StaffService staff; private final VerificationService verification; private final CashIssueService cash; private final AdminService admin;
  public StaffController(StaffService staff,VerificationService verification,CashIssueService cash,AdminService admin){this.staff=staff;this.verification=verification;this.cash=cash;this.admin=admin;}
  public record ParseRequest(@NotBlank String token){}
  public record VerifyRequest(@NotBlank String requestNo,@NotBlank String cardCodeToken,@NotNull Long cardId,@NotBlank String benefitId,@NotNull @DecimalMin("0.01") BigDecimal quantity,@NotBlank @Size(max=50) String operatorName){}
  public record CashRequest(@NotBlank String requestNo,@NotBlank String memberCodeToken,@NotNull Long productId,@NotNull @DecimalMin("0.01") BigDecimal paidAmount){}
  @PostMapping("/member-codes/parse") public ApiResponse<Map<String,Object>> parseMember(@Valid @RequestBody ParseRequest r){staff.require(RequestActor.memberId(),true);return ApiResponse.ok(cash.parseMember(r.token()));}
  @PostMapping("/card-codes/parse") public ApiResponse<Map<String,Object>> parseCard(@Valid @RequestBody ParseRequest r){staff.require(RequestActor.memberId(),false);return ApiResponse.ok(verification.parse(r.token()));}
  @PostMapping("/verifications") public ApiResponse<Map<String,Object>> verify(@Valid @RequestBody VerifyRequest r){long id=RequestActor.memberId();staff.require(id,false);return ApiResponse.ok(verification.verify(id,r.requestNo(),r.cardCodeToken(),r.cardId(),r.benefitId(),r.quantity(),r.operatorName()));}
  @PostMapping("/cash-orders") public ApiResponse<Map<String,Object>> cash(@Valid @RequestBody CashRequest r){long id=RequestActor.memberId();staff.require(id,true);return ApiResponse.ok(cash.issue(id,r.requestNo(),r.memberCodeToken(),r.productId(),r.paidAmount()));}
  @GetMapping("/verifications") public ApiResponse<List<Verification>> records(){staff.require(RequestActor.memberId(),false);return ApiResponse.ok(verification.records());}
  @GetMapping("/reports/summary") public ApiResponse<Map<String,Object>> summary(){staff.require(RequestActor.memberId(),true);return ApiResponse.ok(admin.overview());}
}
