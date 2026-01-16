package org.example.campusclaim.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.campusclaim.entity.User; 

@Mapper
public interface UserMapper extends BaseMapper<User> { // 泛型用 User 或 Object

    @Select("SELECT email FROM users WHERE user_id = #{userId}")
    String selectEmailByUserId(String userId);
    
}