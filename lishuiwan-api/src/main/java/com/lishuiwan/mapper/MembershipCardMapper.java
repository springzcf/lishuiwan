package com.lishuiwan.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lishuiwan.domain.MembershipCard;
import org.apache.ibatis.annotations.Select;
public interface MembershipCardMapper extends BaseMapper<MembershipCard> {
  @Select("SELECT * FROM t_membership_card WHERE id=#{id} FOR UPDATE") MembershipCard selectForUpdate(long id);
}
