package com.lishuiwan.security;

import com.lishuiwan.common.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import com.lishuiwan.mapper.MemberMapper;
import com.lishuiwan.mapper.AdminUserMapper;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {
  private final TokenService tokens;
  private final com.fasterxml.jackson.databind.ObjectMapper mapper;
  private final MemberMapper members; private final AdminUserMapper admins;
  public ApiAuthFilter(TokenService tokens, com.fasterxml.jackson.databind.ObjectMapper mapper,MemberMapper members,AdminUserMapper admins) { this.tokens = tokens; this.mapper = mapper; this.members=members; this.admins=admins; }

  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String traceId = req.getHeader("X-Trace-Id"); if (traceId == null || !traceId.matches("[A-Za-z0-9_-]{8,64}")) traceId = TraceIds.create();
    MDC.put("traceId", traceId); res.setHeader("X-Trace-Id", traceId);
    try {
      String path = req.getRequestURI();
      if (path.startsWith("/api/public/") || path.startsWith("/actuator/")) { chain.doFilter(req,res); return; }
      if (!path.startsWith("/api/")) { chain.doFilter(req,res); return; }
      String header = req.getHeader("Authorization");
      if (header == null || !header.startsWith("Bearer ")) throw BizException.unauthorized();
      Claims claims = tokens.parse(header.substring(7));
      String expected = path.startsWith("/api/admin/") ? "admin" : "member";
      if (!expected.equals(claims.get("type", String.class))) throw BizException.forbidden();
      long actorId=Long.parseLong(claims.getSubject());
      if("member".equals(expected)){var actor=members.selectById(actorId);if(actor==null||actor.getStatus()!=0)throw BizException.unauthorized();}
      else{var actor=admins.selectById(actorId);if(actor==null||actor.getStatus()!=0)throw BizException.unauthorized();}
      RequestActor.set(new RequestActor(expected, actorId));
      chain.doFilter(req,res);
    } catch (BizException e) { write(res,e.getStatus().value(),e.getCode(),e.getMessage());
    } catch (Exception e) { write(res,401,40001,"登录已失效");
    } finally { RequestActor.clear(); MDC.clear(); }
  }
  private void write(HttpServletResponse res, int status, int code, String msg) throws IOException {
    res.setStatus(status); res.setContentType(MediaType.APPLICATION_JSON_VALUE); res.setCharacterEncoding("UTF-8");
    mapper.writeValue(res.getWriter(), new ApiResponse<>(code,msg,null,TraceIds.current()));
  }
}
