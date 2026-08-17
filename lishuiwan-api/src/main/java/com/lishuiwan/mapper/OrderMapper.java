package com.lishuiwan.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import com.lishuiwan.domain.OrderEntity; import org.apache.ibatis.annotations.Select;
public interface OrderMapper extends BaseMapper<OrderEntity> { @Select("SELECT * FROM t_order WHERE order_no=#{orderNo} FOR UPDATE") OrderEntity selectForUpdate(String orderNo); }
