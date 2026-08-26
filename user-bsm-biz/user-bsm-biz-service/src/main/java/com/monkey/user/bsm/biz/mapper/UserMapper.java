package com.monkey.user.bsm.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.monkey.user.bsm.biz.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author gkk
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
