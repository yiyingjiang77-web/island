package com.fruitisland.user.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.user.entity.UserLogin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserLoginMapper extends BaseMapperX<UserLogin> {

    @Select("SELECT * FROM user_login WHERE platform = #{platform} AND platform_uid = #{platformUid} LIMIT 1")
    UserLogin selectByPlatformAndUid(@Param("platform") String platform,
                                     @Param("platformUid") String platformUid);
}
