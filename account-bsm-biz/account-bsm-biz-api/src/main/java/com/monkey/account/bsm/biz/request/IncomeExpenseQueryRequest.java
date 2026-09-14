package com.monkey.account.bsm.biz.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 收支流水查询请求
 */
@Data
public class IncomeExpenseQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID（由 Controller 从登录会话回填，调用方无需传入） */
    private String userId;

    /** 页码，默认 1 */
    private Integer pageNum;

    /** 每页条数，默认 20，最大 100 */
    private Integer pageSize;
}
