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

    /**
     * 按「货主 + 运单号」查询托管中的运费冻结明细（供清算服务回填冻结流水号）
     * <p>
     * 异常统一兜底为失败结果，避免账户域异常向上抛出打断调用方（清算、结算）流程。
     *
     * @param userId  用户ID（货主）
     * @param orderNo 关联单号（运单号）
     * @return 冻结明细，查无记录或异常时返回失败结果
     */
    @Override
    public Result<FrozenDetailDto> selectTransportFrozenDetail(String userId, String orderNo) {
        try {
            return frozenDetailService.selectTransportFrozenDetail(userId, orderNo);
        } catch (Exception e) {
            log.error("查询运单运费托管冻结明细失败: userId={}, orderNo={}", userId, orderNo, e);
            return Result.fail("查询运单运费托管冻结明细失败");
        }
    }
}
