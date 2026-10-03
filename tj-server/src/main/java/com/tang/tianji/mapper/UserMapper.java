package com.tang.tianji.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tang.tianji.entity.User;
import org.apache.ibatis.annotations.*;

public interface UserMapper extends BaseMapper<User> {
  @Select("SELECT id FROM app_user WHERE id=#{id} FOR UPDATE")
  Long lock(@Param("id") long id);
}
