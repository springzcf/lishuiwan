package com.lishuiwan.service;

import com.lishuiwan.common.*; import com.lishuiwan.domain.Member; import com.lishuiwan.mapper.MemberMapper;
import org.springframework.stereotype.Service;

@Service
public class StaffService {
  private final MemberMapper members;
  public StaffService(MemberMapper members){this.members=members;}
  public Member require(long id,boolean admin){Member m=members.selectById(id);if(m==null||m.getStatus()!=0)throw BizException.unauthorized();boolean allowed=admin?"admin".equals(m.getStaffRole()):("admin".equals(m.getStaffRole())||"verifier".equals(m.getStaffRole()));if(!allowed)throw BizException.forbidden();return m;}
}
