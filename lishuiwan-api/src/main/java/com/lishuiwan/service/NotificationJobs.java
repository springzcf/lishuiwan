package com.lishuiwan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; import com.lishuiwan.config.AppProperties; import com.lishuiwan.domain.*; import com.lishuiwan.mapper.*;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Service; import java.time.format.DateTimeFormatter;

@Service
public class NotificationJobs {
  private final NotificationMapper notifications; private final MemberMapper members; private final WechatClient wechat; private final AppProperties p;
  public NotificationJobs(NotificationMapper notifications,MemberMapper members,WechatClient wechat,AppProperties p){this.notifications=notifications;this.members=members;this.wechat=wechat;this.p=p;}
  @Scheduled(fixedDelay=30000) public void push(){for(Notification n:notifications.selectList(new LambdaQueryWrapper<Notification>().eq(Notification::getPushStatus,"pending").lt(Notification::getPushAttempts,2).orderByAsc(Notification::getId).last("limit 50"))){String template="verify".equals(n.getType())?p.getWechat().getVerifyTemplateId():p.getWechat().getIssueTemplateId();if(template==null||template.isBlank()){n.setPushStatus("not_configured");n.setPushLastError("template id not configured");notifications.updateById(n);continue;}try{Member m=members.selectById(n.getMemberId());int code=wechat.sendSubscribe(m.getOpenid(),template,"pages/notifications/index",n.getTitle()+" "+n.getContent(),n.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));n.setPushAttempts(n.getPushAttempts()+1);if(code==0)n.setPushStatus("sent");else if(code==43101)n.setPushStatus("not_authorized");else{n.setPushLastError("wechat errcode="+code);if(n.getPushAttempts()>=2)n.setPushStatus("failed");}}catch(Exception e){n.setPushAttempts(n.getPushAttempts()+1);n.setPushLastError(e.getClass().getSimpleName());if(n.getPushAttempts()>=2)n.setPushStatus("failed");}notifications.updateById(n);}}
}
