package com.lishuiwan.common;

public record RequestActor(String type, long id) {
  private static final ThreadLocal<RequestActor> HOLDER = new ThreadLocal<>();
  public static void set(RequestActor actor) { HOLDER.set(actor); }
  public static RequestActor get() { if (HOLDER.get() == null) throw BizException.unauthorized(); return HOLDER.get(); }
  public static long memberId() { RequestActor a = get(); if (!"member".equals(a.type)) throw BizException.forbidden(); return a.id; }
  public static long adminId() { RequestActor a = get(); if (!"admin".equals(a.type)) throw BizException.forbidden(); return a.id; }
  public static void clear() { HOLDER.remove(); }
}
