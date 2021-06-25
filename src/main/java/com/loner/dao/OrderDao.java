package com.loner.dao;

import com.loner.domain.MiaoshaOrder;
import com.loner.domain.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;

@Mapper
public interface OrderDao {

    @Select("select * from miaosha_order where user_id=#{uId} and goods_id=#{gId}")
    public Order getOrderByUidGid(long uId,long gId);

    @Insert("insert into order_info(user_id,goods_id,goods_name,goods_count,goods_price,order_channel," +
            "status,create_date) values(#{userId},#{goodsId},#{goodsName},#{goodsCount}," +
            "#{goodsPrice},#{orderChannel},#{status},#{createDate})")
    @SelectKey(keyColumn="id", keyProperty="id", resultType=long.class, before=false, statement="select last_insert_id()")
    public long insertOrder(Order order);

    @Insert("insert into miaosha_order(user_id, goods_id, order_id)values(#{userId}, #{goodsId}, #{orderId})")
    public int insertMiaoshaOrder(MiaoshaOrder miaoshaOrder);

    @Select("select * from order_info where id=#{orderId}")
    public Order getOrderByOrderId(long orderId);
}
