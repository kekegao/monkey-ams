package com.monkey.account.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.FrozenDetailProtocol;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.account.bsm.biz.service.inf.FrozenDetailService;
import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 账户冻结明细 协议实现
 */
@Slf4j
@DubboService
public class FrozenDetailProtocolImpl implements FrozenDetailProtocol {

    @Autowired
    private FrozenDetailService frozenDetailService;

    @Override
    public Result<List<FrozenDetailDto>> selectFrozenDetailList(String userId) {
        return frozenDetailService.selectFrozenDetailList(userId);
    }
}
