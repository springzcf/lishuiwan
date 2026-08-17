package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lishuiwan.common.*; import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*;
import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.*;

@Service
public class AdminService {
  private final CardProductMapper products; private final ActivityMapper activities; private final MemberMapper members; private final OrderMapper orders;
  private final VerificationMapper verifications; private final AdminUserMapper admins; private final OperationLogMapper logs; private final Jsons json; private final PasswordEncoder passwords; private final OrderItemMapper items;
  public AdminService(CardProductMapper products,ActivityMapper activities,MemberMapper members,OrderMapper orders,VerificationMapper verifications,AdminUserMapper admins,OperationLogMapper logs,Jsons json,PasswordEncoder passwords,OrderItemMapper items){this.products=products;this.activities=activities;this.members=members;this.orders=orders;this.verifications=verifications;this.admins=admins;this.logs=logs;this.json=json;this.passwords=passwords;this.items=items;}
  @Transactional public CardProduct saveProduct(Long id,Map<String,Object> b,long adminId){CardProduct p=id==null?new CardProduct():products.selectById(id);if(p==null)throw BizException.notFound("卡商品");p.setName(text(b,"name"));p.setCategory(text(b,"category"));p.setCover(nullable(b,"cover"));p.setBenefits(json.write(b.get("benefits")));p.setPrice(money(b,"price"));p.setSalePrice(money(b,"salePrice"));p.setActivityPrice(optionalMoney(b,"activityPrice"));p.setActivityStartAt(date(b,"activityStartAt"));p.setActivityEndAt(date(b,"activityEndAt"));p.setValidDays(integer(b,"validDays",null));p.setRules(nullable(b,"rules"));p.setStatus(integer(b,"status",0));p.setSort(integer(b,"sort",0));validateProduct(p,b.get("benefits"));if(id==null)products.insert(p);else products.updateById(p);log(adminId,id==null?"create_product":"update_product","product",p.getId(),b);return p;}
  void validateProduct(CardProduct p,Object benefits){
    if(p.getName().length()>100||p.getCategory().length()>20||p.getValidDays()<=0||p.getValidDays()>3650||p.getPrice().compareTo(p.getSalePrice())<0||!validMoney(p.getPrice())||!validMoney(p.getSalePrice())||!Set.of(0,1).contains(p.getStatus()))throw bad("商品价格、状态或有效期不合法");
    if(p.getCover()!=null&&p.getCover().length()>255||p.getRules()!=null&&p.getRules().length()>500)throw bad("商品图片地址或规则过长");
    if(!(benefits instanceof List<?> l)||l.isEmpty()||l.size()>50)throw bad("权益数量必须为 1 至 50 项");
    Set<String> ids=new HashSet<>();
    for(Object o:l){
      if(!(o instanceof Map<?,?> m))throw bad("权益配置格式错误");
      String id=value(m.get("benefitId")),item=value(m.get("item")),type=value(m.get("type"));
      if(!id.matches("[A-Za-z0-9_-]{1,64}")||item.isBlank()||item.length()>100||!Set.of("times","hours","unlimited").contains(type)||!ids.add(id))throw bad("权益配置不完整、类型错误或 benefitId 重复");
      if(!"unlimited".equals(type)){
        BigDecimal total=decimal(m.get("total"),"权益总量"),step=m.get("step")==null?new BigDecimal("times".equals(type)?"1":"0.5"):decimal(m.get("step"),"核销步长");
        if(total.compareTo(BigDecimal.ZERO)<=0||step.compareTo(BigDecimal.ZERO)<=0||decimalPlaces(total)>2||decimalPlaces(step)>2||total.compareTo(new BigDecimal("99999999.99"))>0||total.remainder(step).compareTo(BigDecimal.ZERO)!=0)throw bad("权益总量或核销步长不合法");
      }
    }
    if(p.getActivityPrice()!=null&&(p.getActivityStartAt()==null||p.getActivityEndAt()==null||!p.getActivityStartAt().isBefore(p.getActivityEndAt())||!validMoney(p.getActivityPrice())||p.getActivityPrice().compareTo(p.getSalePrice())>0))throw bad("活动价配置不合法");
  }
  @Transactional public void productStatus(long id,int status,long adminId){if(!Set.of(0,1).contains(status))throw bad("商品状态不合法");CardProduct p=products.selectById(id);if(p==null)throw BizException.notFound("卡商品");p.setStatus(status);products.updateById(p);log(adminId,"product_status","product",id,Map.of("status",p.getStatus()));}
  @Transactional public void deleteProduct(long id,long adminId){if(products.deleteById(id)==0)throw BizException.notFound("卡商品");log(adminId,"delete_product","product",id,Map.of());}
  @Transactional public Activity saveActivity(Long id,Activity a,long adminId){validateActivity(a);if(id==null)activities.insert(a);else{a.setId(id);activities.updateById(a);}log(adminId,id==null?"create_activity":"update_activity","activity",a.getId(),Map.of("title",a.getTitle()));return a;}
  void validateActivity(Activity a){if(a.getTitle()==null||a.getTitle().isBlank()||a.getTitle().length()>100||a.getImage()==null||a.getImage().isBlank()||a.getImage().length()>255||a.getStartAt()==null||a.getEndAt()==null||!a.getStartAt().isBefore(a.getEndAt())||a.getTargetType()==null||!Set.of("none","product").contains(a.getTargetType())||a.getStatus()==null||!Set.of(0,1).contains(a.getStatus()))throw bad("活动信息不完整或不合法");if("product".equals(a.getTargetType())&&a.getTargetId()==null)throw bad("商品活动必须指定目标商品");}
  @Transactional public void deleteActivity(long id,long adminId){if(activities.deleteById(id)==0)throw BizException.notFound("活动");log(adminId,"delete_activity","activity",id,Map.of());}
  @Transactional public void grant(long adminId,long memberId,String role){if(!Set.of("customer","verifier","admin").contains(role))throw bad("角色不合法");Member m=members.selectById(memberId);if(m==null)throw BizException.notFound("会员");String before=m.getStaffRole();m.setStaffRole(role);members.updateById(m);log(adminId,"grant_role","member",memberId,Map.of("before",before,"after",role));}
  @Transactional public AdminUser saveAdmin(Long id,String username,String password,String name,Integer status,long operator){AdminUser a=id==null?new AdminUser():admins.selectById(id);if(a==null)throw BizException.notFound("管理员");if(username.length()>50||name.length()>50||status!=null&&!Set.of(0,1).contains(status))throw bad("管理员信息不合法");a.setUsername(username);a.setName(name);a.setStatus(status==null?0:status);if(password!=null&&!password.isBlank()){if(password.length()<12||password.length()>72)throw bad("密码长度必须为 12 至 72 位");a.setPassword(passwords.encode(password));}if(id==null&&a.getPassword()==null)throw bad("请输入密码");if(id==null)admins.insert(a);else admins.updateById(a);log(operator,id==null?"create_admin":"update_admin","admin_user",a.getId(),Map.of("username",username));return a;}
  @Transactional public void disableAdmin(long id,long operator){AdminUser a=admins.selectById(id);if(a==null)throw BizException.notFound("管理员");a.setStatus(1);admins.updateById(a);log(operator,"disable_admin","admin_user",id,Map.of());}
  public Map<String,Object> overview(){LocalDateTime start=LocalDateTime.now().toLocalDate().atStartOfDay();List<OrderEntity> paid=orders.selectList(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getPayStatus,1));BigDecimal sales=paid.stream().map(OrderEntity::getPaidAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);return Map.of("sales",sales,"orders",paid.size(),"verifications",verifications.selectCount(null),"members",members.selectCount(null),"todayNewMembers",members.selectCount(new LambdaQueryWrapper<Member>().ge(Member::getCreatedAt,start)));}
  public List<Map<String,Object>> salesTrend(){return orders.selectList(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getPayStatus,1).orderByAsc(OrderEntity::getPaidAt)).stream().collect(java.util.stream.Collectors.groupingBy(o->o.getPaidAt().toLocalDate().toString(),LinkedHashMap::new,java.util.stream.Collectors.reducing(BigDecimal.ZERO,OrderEntity::getPaidAmount,BigDecimal::add))).entrySet().stream().map(e->Map.<String,Object>of("date",e.getKey(),"amount",e.getValue())).toList();}
  @SuppressWarnings("unchecked") public List<Map<String,Object>> categoryReport(){Map<String,Long> grouped=new LinkedHashMap<>();for(OrderEntity o:orders.selectList(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getPayStatus,1))){OrderItem item=items.selectOne(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId,o.getId()));if(item!=null){Map<String,Object>s=json.read(item.getProductSnapshot(),Map.class);grouped.merge(String.valueOf(s.getOrDefault("category","其他")),1L,Long::sum);}}return grouped.entrySet().stream().map(e->Map.<String,Object>of("category",e.getKey(),"count",e.getValue())).toList();}
  private void log(long operator,String action,String type,Long target,Object detail){OperationLog l=new OperationLog();l.setOperatorType("admin_user");l.setOperatorId(operator);l.setAction(action);l.setTargetType(type);l.setTargetId(target);l.setDetail(json.write(detail));logs.insert(l);}
  private String text(Map<String,Object>b,String k){String v=nullable(b,k);if(v==null||v.isBlank())throw new BizException(40002,k+"不能为空",HttpStatus.BAD_REQUEST);return v;}
  private String nullable(Map<String,Object>b,String k){Object v=b.get(k);return v==null?null:v.toString();}
  private BigDecimal money(Map<String,Object>b,String k){return decimal(b.get(k),k);}
  private BigDecimal optionalMoney(Map<String,Object>b,String k){return b.get(k)==null||b.get(k).toString().isBlank()?null:decimal(b.get(k),k);}
  private BigDecimal decimal(Object v,String name){try{if(v==null)throw new NumberFormatException();return new BigDecimal(v.toString());}catch(Exception e){throw bad(name+"格式错误");}}
  private int integer(Map<String,Object>b,String k,Integer fallback){try{Object v=b.get(k);if(v==null&&fallback!=null)return fallback;if(v==null)throw new NumberFormatException();return Integer.parseInt(v.toString());}catch(Exception e){throw bad(k+"格式错误");}}
  private LocalDateTime date(Map<String,Object>b,String k){try{return b.get(k)==null||b.get(k).toString().isBlank()?null:LocalDateTime.parse(b.get(k).toString());}catch(Exception e){throw bad(k+"格式错误");}}
  private boolean validMoney(BigDecimal v){return v!=null&&v.compareTo(BigDecimal.ZERO)>0&&decimalPlaces(v)<=2&&v.compareTo(new BigDecimal("99999999.99"))<=0;}
  private int decimalPlaces(BigDecimal v){return Math.max(0,v.stripTrailingZeros().scale());}
  private String value(Object v){return v==null?"":v.toString().trim();}
  private BizException bad(String message){return new BizException(40002,message,HttpStatus.BAD_REQUEST);}
}
