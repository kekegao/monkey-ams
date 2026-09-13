package com.monkey.order.bsm.biz.protocol;

import com.monkey.ams.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService
public class OrderJobProtocolImpl implements OrderJobProtocol{



    @Override
    public Result settleApplyTask() {



        return null;
    }
}
