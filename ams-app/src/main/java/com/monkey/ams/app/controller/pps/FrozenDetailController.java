package com.monkey.ams.app.controller.pps;

import com.monkey.account.bsm.biz.api.FrozenDetailProtocol;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.ams.app.controller.BaseController;
import com.monkey.ams.common.response.Result;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
public class FrozenDetailController extends BaseController {

    @DubboReference
    private FrozenDetailProtocol frozenDetailProtocol;

    /**
     * 冻结明细查询（当前登录用户，仅返回「冻结中」记录）
     * 完整调用链：
     * account.vue -> ams-app(FrozenDetailController.list) -> account-bsm-biz-service(FrozenDetailProtocolImpl)
     *   -> FrozenDetailServiceImpl -> FrozenDetailMapper(selectFrozenDetailList) -> MyBatis -> tf_b_frozen_detail 表
     *
     * POST /frozenDetail/list
     */
    @PostMapping("/list")
    public Result<List<FrozenDetailDto>> queryFrozenDetailList() {
        return frozenDetailProtocol.selectFrozenDetailList(getUserId());
    }

}
