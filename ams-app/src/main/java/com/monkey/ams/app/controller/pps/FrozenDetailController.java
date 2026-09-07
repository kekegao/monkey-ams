package com.monkey.ams.app.controller.pps;

import com.monkey.account.bsm.biz.api.FrozenDetailProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 账户冻结明细记录表 前端控制器
 * </p>
 *
 * @author gkk
 * @since 2026-09-07
 */
@RestController
@RequestMapping("/frozenDetail")
public class FrozenDetailController {

    @DubboReference
    private FrozenDetailProtocol frozenDetailProtocol;

}
