package com.airticket.mapper;

import com.airticket.model.User;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Insert("INSERT INTO users (username, password, email, full_name, phone, role, avatar_url) " +
            "VALUES (#{username}, #{password}, #{email}, #{fullName}, #{phone}, #{role}, #{avatarUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET password = #{password}, email = #{email}, full_name = #{fullName}, phone = #{phone}, role = #{role}, avatar_url = #{avatarUrl} " +
            "WHERE id = #{id}")
    int update(User user);
    
    @Update("UPDATE users SET email = #{email}, full_name = #{fullName}, phone = #{phone} " +
            "WHERE id = #{id}")
    int updateProfile(User user);

    @Update("UPDATE users SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatar(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);
    
    @Delete("DELETE FROM users WHERE role != 'ADMIN'")
    int deleteAllExceptAdmin();
    
    // Try to add balance, but handle gracefully if column doesn't exist
    @Update("UPDATE users SET balance = COALESCE(balance, 0) + #{amount} WHERE id = #{userId}")
    int addToBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    // Try to deduct balance, but handle gracefully if column doesn't exist
    @Update("UPDATE users SET balance = COALESCE(balance, 0) - #{amount} WHERE id = #{userId} AND COALESCE(balance, 0) >= #{amount}")
    int deductFromBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
    
    // Try to get balance, but return 0 if column doesn't exist
    @Select("SELECT COALESCE(balance, 0) FROM users WHERE id = #{userId}")
    BigDecimal getBalance(Long userId);
    
    // Add balance column to users table (will fail silently if column exists)
    @Update("ALTER TABLE users ADD COLUMN balance DECIMAL(10,2) DEFAULT 0.00")
    void addBalanceColumn();
}