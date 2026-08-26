package com.monkey.account.bsm.biz.protocol;

import com.monkey.account.bsm.biz.api.AccountProtocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

@Slf4j
@DubboService(version = "1.0.0", group = "dev", timeout = 5000)
public class AccountProtocolImpl implements AccountProtocol {



}
