package com.monkey.user.bsm.biz.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.monkey.user.bsm.biz.entity.UserEntity;
import com.monkey.user.bsm.biz.mapper.UserMapper;
import com.monkey.user.bsm.biz.service.inf.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author gkk
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {
}
