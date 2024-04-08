package com.fm.bingo.processor;

import com.fm.bingo.RemotingProcessor;
import com.fm.bingo.protocol.MessageType;
import com.fm.bingo.protocol.RpcMessage;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHeartbeatProcessor implements RemotingProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHeartbeatProcessor.class);

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        if (rpcMessage.getType() == MessageType.TYPE_HEARTBEAT_MSG) {
            LOGGER.info("receive heartbeat message from server");
        }
    }
}
