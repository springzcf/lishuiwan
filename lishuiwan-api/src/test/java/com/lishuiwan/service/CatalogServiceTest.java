package com.lishuiwan.service;

import com.lishuiwan.domain.CardProduct;
import org.junit.jupiter.api.Test; import java.math.BigDecimal; import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceTest {
  private final CatalogService service=new CatalogService(null,null,null);
  @Test void usesActivePromotionPrice(){CardProduct p=new CardProduct();p.setSalePrice(new BigDecimal("88.00"));p.setActivityPrice(new BigDecimal("68.00"));p.setActivityStartAt(LocalDateTime.now().minusMinutes(1));p.setActivityEndAt(LocalDateTime.now().plusMinutes(1));assertThat(service.currentPrice(p)).isEqualByComparingTo("68.00");}
  @Test void fallsBackToSalePriceOutsidePromotion(){CardProduct p=new CardProduct();p.setSalePrice(new BigDecimal("88.00"));p.setActivityPrice(new BigDecimal("68.00"));p.setActivityStartAt(LocalDateTime.now().minusDays(2));p.setActivityEndAt(LocalDateTime.now().minusDays(1));assertThat(service.currentPrice(p)).isEqualByComparingTo("88.00");}
}
