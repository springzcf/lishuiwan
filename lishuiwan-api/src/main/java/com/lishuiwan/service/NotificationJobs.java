package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; import com.lishuiwan.config.AppProperties; import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Service; import java.time.format.DateTimeFormatter;

@Service
public class NotificationJobs {
  private final NotificationMapper notifications; private final WechatClient wechat; private final AppProperties p; private final WechatIdentityService identities;
  public NotificationJobs(NotificationMapper notifications,WechatClient wechat,AppProperties p,WechatIdentityService identities){this.notifications=notifications;this.wechat=wechat;this.p=p;this.identities=identities;}
  @Scheduled(fixedDelay=30000) public void push(){for(Notification n:notifications.selectList(new LambdaQueryWrapper<Notification>().eq(Notification::getPushStatus,"pending").lt(Notification::getPushAttempts,2).orderByAsc(Notification::getId).last("limit 50"))){String template="verify".equals(n.getType())?p.getWechat().getVerifyTemplateId():p.getWechat().getIssueTemplateId();if(template==null||template.isBlank()){n.setPushStatus("not_configured");n.setPushLastError("template id not configured");notifications.updateById(n);continue;}String miniOpenid=identities.findOpenid(n.getMemberId(),WechatIdentityService.MINI_PROGRAM);if(miniOpenid==null){n.setPushStatus("not_bound");n.setPushLastError("mini program identity not bound");notifications.updateById(n);continue;}try{int code=wechat.sendSubscribe(miniOpenid,template,"pages/notifications/index",n.getTitle()+" "+n.getContent(),n.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));n.setPushAttempts(n.getPushAttempts()+1);if(code==0)n.setPushStatus("sent");else if(code==43101)n.setPushStatus("not_authorized");else{n.setPushLastError("wechat errcode="+code);if(n.getPushAttempts()>=2)n.setPushStatus("failed");}}catch(Exception e){n.setPushAttempts(n.getPushAttempts()+1);n.setPushLastError(e.getClass().getSimpleName());if(n.getPushAttempts()>=2)n.setPushStatus("failed");}notifications.updateById(n);}}
}
