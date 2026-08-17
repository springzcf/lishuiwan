package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lishuiwan.common.BizException;
import com.lishuiwan.common.Jsons;
import com.lishuiwan.domain.Activity;
import com.lishuiwan.domain.CardProduct;
import com.lishuiwan.mapper.ActivityMapper;
import com.lishuiwan.mapper.CardProductMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.*;

@Service
public class CatalogService {
  private final CardProductMapper products; private final ActivityMapper activities; private final Jsons json;
  public CatalogService(CardProductMapper products,ActivityMapper activities,Jsons json){this.products=products;this.activities=activities;this.json=json;}
  public List<Map<String,Object>> products(boolean onlyActive){
    LambdaQueryWrapper<CardProduct> q=new LambdaQueryWrapper<CardProduct>().orderByAsc(CardProduct::getSort).orderByDesc(CardProduct::getId);
    if(onlyActive)q.eq(CardProduct::getStatus,1); return products.selectList(q).stream().map(this::view).toList();
  }
  public CardProduct requireProduct(long id, boolean active){CardProduct p=products.selectById(id);if(p==null||(active&&p.getStatus()!=1))throw BizException.notFound("卡商品");return p;}
  public Map<String,Object> product(long id){return view(requireProduct(id,true));}
  public Map<String,Object> home(){LocalDateTime n=LocalDateTime.now();List<Activity> a=activities.selectList(new LambdaQueryWrapper<Activity>().eq(Activity::getStatus,1).le(Activity::getStartAt,n).gt(Activity::getEndAt,n).orderByAsc(Activity::getSort));return Map.of("activities",a,"products",products(true));}
  public BigDecimal currentPrice(CardProduct p){LocalDateTime n=LocalDateTime.now();return p.getActivityPrice()!=null&&p.getActivityStartAt()!=null&&p.getActivityEndAt()!=null&&!n.isBefore(p.getActivityStartAt())&&n.isBefore(p.getActivityEndAt())?p.getActivityPrice():p.getSalePrice();}
  public Map<String,Object> view(CardProduct p){Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.getId());m.put("name",p.getName());m.put("category",p.getCategory());m.put("cover",p.getCover());m.put("benefits",json.benefits(p.getBenefits()));m.put("price",p.getPrice());m.put("salePrice",p.getSalePrice());m.put("activityPrice",p.getActivityPrice());m.put("currentPrice",currentPrice(p));m.put("activityStartAt",p.getActivityStartAt());m.put("activityEndAt",p.getActivityEndAt());m.put("validDays",p.getValidDays());m.put("rules",p.getRules());m.put("status",p.getStatus());m.put("sort",p.getSort());return m;}
}
