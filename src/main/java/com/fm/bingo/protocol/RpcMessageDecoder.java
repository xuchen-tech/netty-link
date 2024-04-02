package com.fm.bingo.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageDecoder() {
        super(Integer.MAX_VALUE, 12, 4);
    }
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded;
        decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            return decodeFrame(frame);
        }
        return decoded;
    }
    public Object decodeFrame(ByteBuf frame) {
        int header = frame.readInt();
        int requestId = frame.readInt();
        int type = frame.readShort();
        int length = frame.readInt();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setHeader(header);
        rpcMessage.setRequestId(requestId);
        rpcMessage.setType(type);
        rpcMessage.setLength(length);
        if (length != 0) {
            byte[] payload = new byte[frame.readableBytes()];
            frame.readBytes(payload);
            rpcMessage.setPayload(payload);
        }

        return rpcMessage;
    }
}
