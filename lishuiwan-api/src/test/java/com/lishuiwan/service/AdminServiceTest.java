package com.lishuiwan.service;

import com.lishuiwan.common.BizException;
import com.lishuiwan.domain.Activity;
import com.lishuiwan.domain.CardProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminServiceTest {
  private AdminService service;

  @BeforeEach void setUp(){
    service=new AdminService(null,null,null,null,null,null,null,null,null,null);
  }

  @Test void acceptsValidBenefitConfiguration(){
    List<Map<String,Object>> benefits=List.of(Map.of("benefitId","pool_hours","item","大池时长","type","hours","total","10.00","step","0.50"));
    assertThatCode(()->service.validateProduct(product(),benefits)).doesNotThrowAnyException();
  }

  @Test void rejectsUnsupportedBenefitType(){
    assertThatThrownBy(()->service.validateProduct(product(),List.of(Map.of("benefitId","x","item","测试","type","money","total",10)))).isInstanceOf(BizException.class).hasMessageContaining("类型错误");
  }

  @Test void rejectsBenefitTotalThatDoesNotMatchStep(){
    assertThatThrownBy(()->service.validateProduct(product(),List.of(Map.of("benefitId","x","item","测试","type","hours","total","1.2","step","0.5")))).isInstanceOf(BizException.class).hasMessageContaining("核销步长");
  }

  @Test void rejectsIncompleteActivityAsBusinessError(){
    Activity a=new Activity();a.setTitle("活动");a.setImage("/banner.png");a.setTargetType("none");a.setStartAt(LocalDateTime.now());a.setEndAt(LocalDateTime.now().plusDays(1));
    assertThatThrownBy(()->service.validateActivity(a)).isInstanceOf(BizException.class).hasMessageContaining("活动信息");
  }

  private CardProduct product(){
    CardProduct p=new CardProduct();p.setName("体验卡");p.setCategory("大池");p.setPrice(new BigDecimal("100.00"));p.setSalePrice(new BigDecimal("88.00"));p.setValidDays(365);p.setStatus(1);return p;
  }
}
