package com.monkey.account.bsm.biz.api;

import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.ams.common.response.Result;

import java.util.List;

public interface FrozenDetailProtocol {

    /**
     * 查询当前用户「冻结中」的冻结明细列表（status=1）
     *
     * @param userId 用户ID
     * @return 冻结明细列表
     */
    Result<List<FrozenDetailDto>> selectFrozenDetailList(String userId);

    /**
     * 按「货主 + 运单号」查询该运单当前仍在托管中的运费冻结明细（biz_type=1 运费托管、status=1 冻结中）
     * <p>
     * 使用场景：冻结流水号在「发布货源冻结运费」时由账户侧生成并仅落库于 tf_b_frozen_detail，
     * 结算申请生成货主清算单时需回填 frozen_no（便于结算(8) 阶段按流水号精确扣划与追溯），
     * 故由清算服务调用本接口反查，账户侧为资金唯一真相源。
     *
     * @param userId  用户ID（货主）
     * @param orderNo 关联单号（运单号）
     * @return 冻结明细（含 frozen_no 与实际冻结金额），查无记录或参数缺失时返回失败结果
     */
    Result<FrozenDetailDto> selectTransportFrozenDetail(String userId, String orderNo);
}
