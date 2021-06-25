package com.loner.dao;

import com.loner.domain.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInfoDao {
    @Select("select * from user_info where id=#{id}")
    public UserInfo getById(@Param("id") long id);
}
