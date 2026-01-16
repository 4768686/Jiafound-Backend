package org.example.campususer.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.campususer.entity.Users;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author 31830
* @description 针对表【users(用户基础信息表)】的数据库操作Mapper
* @createDate 2026-01-06 21:20:36
* @Entity org.example.campususer.entity.Users
*/
public interface UsersMapper extends BaseMapper<Users> {

    /**
     * 原子化扣除用户余额（用于惩罚扣10，允许负数）
     * 使用数据库原子操作，避免并发问题
     * @param userId 用户ID
     * @param deductAmount 扣除金额（正数）
     * @return 影响行数
     */
    @Update("UPDATE users SET coin_balance = coin_balance - #{deductAmount}, update_time = NOW() WHERE user_id = #{userId}")
    int deductCoinBalance(@Param("userId") String userId, @Param("deductAmount") BigDecimal deductAmount);
}




