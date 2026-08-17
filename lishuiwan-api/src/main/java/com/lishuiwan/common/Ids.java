package com.lishuiwan.common;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

public final class Ids {
  private Ids() {}
  public static String orderNo() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase(); }
  public static String nonce() { return UUID.randomUUID().toString().replace("-", ""); }
  public static String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
