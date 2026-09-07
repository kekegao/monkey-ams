package com.monkey.account.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import com.monkey.account.bsm.biz.mapper.FrozenDetailMapper;
import com.monkey.account.bsm.biz.service.inf.FrozenDetailService;
import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 账户冻结明细记录表 服务实现类
 * </p>
 *
 * @author gkk
 * @since 2026-09-07
 */
@Slf4j
@Service
public class FrozenDetailServiceImpl extends ServiceImpl<FrozenDetailMapper, FrozenDetail> implements FrozenDetailService {

    @Override
    public Result<List<FrozenDetailDto>> selectFrozenDetailList(String userId) {
        List<FrozenDetailDto> list = this.baseMapper.selectFrozenDetailList(userId);
        if (list == null) {
            list = Collections.emptyList();
        }
        return Result.success(list);
    }
}
