package com.fm.bingo.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage, ByteBuf byteBuf) throws Exception {
        byteBuf.writeInt(rpcMessage.getHeader());
        byteBuf.writeInt(rpcMessage.getRequestId());
        byteBuf.writeInt(rpcMessage.getType());
        byteBuf.writeInt(rpcMessage.getLength());
        if (rpcMessage.getLength() != 0) {
            byteBuf.writeBytes(rpcMessage.getPayload());
        }
    }
}
