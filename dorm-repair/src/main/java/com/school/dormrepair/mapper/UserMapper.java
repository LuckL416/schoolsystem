package com.school.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.dormrepair.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
