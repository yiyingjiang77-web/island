package com.fruitisland.user.mapper;

import com.fruitisland.common.base.BaseMapperX;
import com.fruitisland.user.entity.UserToken;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserTokenMapper extends BaseMapperX<UserToken> {

    @Select("SELECT * FROM user_token WHERE token = #{token} LIMIT 1")
    UserToken selectByToken(@Param("token") String token);

    @Delete("DELETE FROM user_token WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
