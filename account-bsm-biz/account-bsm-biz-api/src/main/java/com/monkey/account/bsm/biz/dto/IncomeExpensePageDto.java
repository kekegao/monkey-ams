package com.monkey.account.bsm.biz.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 收支流水分页结果
 */
@Data
public class IncomeExpensePageDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<IncomeExpenseDto> records;

    /** 总记录数 */
    private long total;

    /** 总页数 */
    private long pages;

    /** 当前页码 */
    private long current;

    /** 每页大小 */
    private long size;
}
