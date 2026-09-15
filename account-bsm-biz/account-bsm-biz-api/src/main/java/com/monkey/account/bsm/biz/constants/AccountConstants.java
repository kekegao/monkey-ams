package com.monkey.account.bsm.biz.constants;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 账户模块常量：配置键 + 由配置中心下发的运行期常量。
 * <p>
 * 平台公司对公账户用户ID由配置中心下发，故本类交由 Spring 管理（{@code @Component}），
 * 通过 setter 注入后以静态方式暴露，保证资金链路各处读取的是同一份来源。
 */
@Component
public class AccountConstants {

    /**
     * 平台公司对公账户用户ID配置键（货主清算收款账户 / 承运方对账划账出账账户）
     */
    public static final String PLATFORM_COMPANY_USER_ID_KEY = "${account.platform.company-user-id:1}";

    /**
     * 平台公司对公账户用户ID缺省值（配置缺失时兜底）
     */
    public static final String DEFAULT_PLATFORM_COMPANY_USER_ID = "1";

    /**
     * -- GETTER --
     *  平台公司对公账户用户ID（已 trim，永不为 null；未配置时返回空串）
     */
    @Getter
    private static String platformCompanyUserId = DEFAULT_PLATFORM_COMPANY_USER_ID;

    /**
     * 注入平台公司对公账户用户ID。
     * <p>
     * 未配置时货主清算 / 承运方对账划账会因「账户不存在」直接失败，
     * 避免资金流向不确定账户（与资金安全口径一致）。
     *
     * @param platformCompanyUserId 配置中心下发的平台公司对公账户用户ID
     */
    @Value(PLATFORM_COMPANY_USER_ID_KEY)
    public void setPlatformCompanyUserId(String platformCompanyUserId) {
        AccountConstants.platformCompanyUserId = platformCompanyUserId == null ? "" : platformCompanyUserId.trim();
    }

}
