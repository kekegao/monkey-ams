package com.monkey.account.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.account.bsm.biz.dto.FrozenDetailDto;
import com.monkey.account.bsm.biz.entity.FrozenDetail;
import com.monkey.account.bsm.biz.mapper.FrozenDetailMapper;
import com.monkey.account.bsm.biz.service.inf.FrozenDetailService;
import com.monkey.ams.common.constants.BizTypeEnum;
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

    /**
     * 冻结明细状态：1 冻结中/处理中
     */
    private static final int STATUS_FROZEN = 1;

    /**
     * 删除标记：0 正常
     */
    private static final byte DELETE_FLAG_NO = 0;

    @Override
    public Result<List<FrozenDetailDto>> selectFrozenDetailList(String userId) {
        List<FrozenDetailDto> list = this.baseMapper.selectFrozenDetailList(userId);
        if (list == null) {
            list = Collections.emptyList();
        }
        return Result.success(list);
    }

    /**
     * 按「货主 + 运单号」定位该运单当前仍处于托管中的运费冻结记录。
     * <p>
     * 查询条件顺序与 idx_order_no(order_no) 索引对齐，避免全表扫描；
     * 同一运单若存在多笔托管中记录（异常重复冻结），取最新一笔并打告警日志，便于排查。
     *
     * @param userId  用户ID（货主）
     * @param orderNo 关联单号（运单号）
     * @return 冻结明细（含 frozen_no 与实际冻结金额），查无记录时返回失败结果
     */
    @Override
    public Result<FrozenDetailDto> selectTransportFrozenDetail(String userId, String orderNo) {
        if (isBlank(userId) || isBlank(orderNo)) {
            return Result.fail("缺少用户ID或关联单号");
        }
        List<FrozenDetail> frozenDetails = lambdaQuery()
                .eq(FrozenDetail::getUserId, userId)
                .eq(FrozenDetail::getOrderNo, orderNo)
                .eq(FrozenDetail::getBizType, BizTypeEnum.TRANSPORT_MONEY.getValue())
                .eq(FrozenDetail::getStatus, STATUS_FROZEN)
                .eq(FrozenDetail::getDeleteFlag, DELETE_FLAG_NO)
                .orderByDesc(FrozenDetail::getId)
                .list();
        if (frozenDetails == null || frozenDetails.isEmpty()) {
            return Result.fail("未查询到该运单处于托管中的运费冻结记录");
        }
        if (frozenDetails.size() > 1) {
            log.warn("运单存在多笔托管中的运费冻结记录，取最新一笔: userId={}, orderNo={}, count={}",
                    userId, orderNo, frozenDetails.size());
        }
        return Result.success(toDto(frozenDetails.get(0)));
    }

    /**
     * 实体转 DTO：bizType / status 实体为 Integer、DTO 为 Byte，故显式转换避免属性拷贝类型不匹配
     */
    private FrozenDetailDto toDto(FrozenDetail entity) {
        FrozenDetailDto dto = new FrozenDetailDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUserName(entity.getUserName());
        dto.setFrozenNo(entity.getFrozenNo());
        dto.setBizType(entity.getBizType() == null ? null : entity.getBizType().byteValue());
        dto.setBizTypeName(entity.getBizTypeName());
        dto.setOrderNo(entity.getOrderNo());
        dto.setAmount(entity.getAmount());
        dto.setFrozenTime(entity.getFrozenTime());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().byteValue());
        dto.setStatusDesc(entity.getStatusDesc());
        dto.setFinishTime(entity.getFinishTime());
        dto.setRemark(entity.getRemark());
        dto.setDeleteFlag(entity.getDeleteFlag());
        dto.setCreateTime(entity.getCreateTime());
        dto.setCreateName(entity.getCreateName());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setUpdateName(entity.getUpdateName());
        return dto;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
