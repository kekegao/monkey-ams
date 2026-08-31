package com.monkey.ams.common.auth.dubbo;

import com.monkey.ams.common.auth.AuthConstants;
import com.monkey.ams.common.auth.context.UserContext;
import com.monkey.ams.common.auth.model.LoginSession;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;

@Activate(
        group = CommonConstants.PROVIDER
)
public class AuthProviderFilter implements Filter {

    @Override
    public Result invoke(
            Invoker<?> invoker,
            Invocation invocation) {

        try {

            String userId =
                    RpcContext.getServerAttachment()
                            .getAttachment(
                                    AuthConstants.RPC_USER_ID
                            );

            if (userId != null) {

                LoginSession session =
                        new LoginSession();

                session.setUserId(
                        Long.valueOf(userId)
                );

                session.setSessionId(
                        RpcContext.getServerAttachment()
                                .getAttachment(
                                        AuthConstants.RPC_SESSION_ID
                                )
                );

                session.setDeviceId(
                        RpcContext.getServerAttachment()
                                .getAttachment(
                                        AuthConstants.RPC_DEVICE_ID
                                )
                );

                UserContext.set(session);
            }

            return invoker.invoke(invocation);

        } finally {

            UserContext.clear();
        }
    }
}
