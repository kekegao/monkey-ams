package com.monkey.settlement.bsm.biz.service.impl;


import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.monkey.settlement.bsm.biz.entity.SettlementShipperWorkOrder;
import com.monkey.settlement.bsm.biz.mapper.SettlementShipperWorkOrderMapper;
import com.monkey.settlement.bsm.biz.service.inf.SettlementShipperWorkOrderService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 货主清算MQ工单记录表 服务实现类
 * </p>
 *
 * @author gkk
 * @since 2026-09-14
 */
@Service
public class SettlementShipperWorkOrderServiceImpl extends ServiceImpl<SettlementShipperWorkOrderMapper, SettlementShipperWorkOrder> implements SettlementShipperWorkOrderService {

}
