package com.fm.bingo;

import com.fm.bingo.protocol.RpcMessage;
import io.netty.channel.ChannelHandlerContext;

public interface RemotingProcessor {
    void process(ChannelHandlerContext ctx, RpcMessage rpcMessage);
}
