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
        group = CommonConstants.CONSUMER
)
public class AuthConsumerFilter implements Filter {

    @Override
    public Result invoke(
            Invoker<?> invoker,
            Invocation invocation) {

        LoginSession session =
                UserContext.get();

        if (session != null) {

            RpcContext.getClientAttachment()
                    .setAttachment(
                            AuthConstants.RPC_USER_ID,
                            session.getUserId()
                    );

            if (session.getSessionId() != null) {

                RpcContext.getClientAttachment()
                        .setAttachment(
                                AuthConstants.RPC_SESSION_ID,
                                session.getSessionId()
                        );
            }

            if (session.getDeviceId() != null) {

                RpcContext.getClientAttachment()
                        .setAttachment(
                                AuthConstants.RPC_DEVICE_ID,
                                session.getDeviceId()
                        );
            }
            if(session.getUserName() != null){
                RpcContext.getClientAttachment()
                        .setAttachment(
                                AuthConstants.RPC_USER_NAME,
                                session.getUserName());
            }

            if(session.getMobile() != null){
                RpcContext.getClientAttachment()
                        .setAttachment(
                                AuthConstants.RPC_USER_MOBILE,
                                session.getMobile());
            }

        }

        return invoker.invoke(invocation);
    }
}