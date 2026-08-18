package com.lishuiwan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lishuiwan.common.BizException;
import com.lishuiwan.common.Jsons;
import com.lishuiwan.config.AppProperties;
import com.lishuiwan.domain.MembershipCard;
import com.lishuiwan.mapper.ConsumedMemberCodeMapper;
import com.lishuiwan.mapper.MembershipCardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberAndCardCodeServiceTest {
  private MemberCodeService memberCodes;
  private CardCodeService cardCodes;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    AppProperties properties = new AppProperties();
    properties.getJwt().setSecret("test-secret-that-is-long-enough");
    QrCodeService qrCodes = new QrCodeService();
    memberCodes = new MemberCodeService(qrCodes, properties);

    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    ValueOperations<String,String> values = mock(ValueOperations.class);
    Map<String,String> storage = new HashMap<>();
    when(redis.opsForValue()).thenReturn(values);
    doAnswer(invocation -> { storage.put(invocation.getArgument(0), invocation.getArgument(1)); return null; })
        .when(values).set(anyString(), anyString(), any(Duration.class));
    when(values.get(anyString())).thenAnswer(invocation -> storage.get(invocation.getArgument(0)));

    ConsumedMemberCodeMapper consumed = mock(ConsumedMemberCodeMapper.class);
    when(consumed.selectCount(any())).thenReturn(0L);
    MembershipCardMapper cards = mock(MembershipCardMapper.class);
    MembershipCard card = new MembershipCard();
    card.setId(9L); card.setMemberId(3L); card.setStatus("using"); card.setValidUntil(LocalDateTime.now().plusDays(1));
    when(cards.selectById(9L)).thenReturn(card);
    cardCodes = new CardCodeService(redis, consumed, cards, new Jsons(new ObjectMapper()), qrCodes, properties);
  }

  @Test
  void memberIdentityCodeIsStableAndReusable() {
    String first = String.valueOf(memberCodes.issue(3L).get("token"));
    String second = String.valueOf(memberCodes.issue(3L).get("token"));

    assertThat(second).isEqualTo(first);
    assertThat(memberCodes.validate(first).memberId()).isEqualTo(3L);
    assertThat(memberCodes.validate(first).memberId()).isEqualTo(3L);
  }

  @Test
  void cardCodeIsDynamicAndBoundToOneCard() {
    String first = String.valueOf(cardCodes.issue(3L, 9L).get("token"));
    String second = String.valueOf(cardCodes.issue(3L, 9L).get("token"));

    assertThat(second).isNotEqualTo(first);
    CardCodeService.CodeData parsed = cardCodes.validate(first);
    assertThat(parsed.memberId()).isEqualTo(3L);
    assertThat(parsed.cardId()).isEqualTo(9L);
  }

  @Test
  void codePurposesCannotBeMixed() {
    String memberToken = String.valueOf(memberCodes.issue(3L).get("token"));
    String cardToken = String.valueOf(cardCodes.issue(3L, 9L).get("token"));

    assertThatThrownBy(() -> memberCodes.validate(cardToken)).isInstanceOf(BizException.class).hasMessageContaining("身份码");
    assertThatThrownBy(() -> cardCodes.validate(memberToken)).isInstanceOf(BizException.class).hasMessageContaining("核销码");
  }
}
