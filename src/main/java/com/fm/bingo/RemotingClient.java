package com.fm.bingo;

import com.fm.bingo.protocol.RpcMessage;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

public interface RemotingClient {
    Object sendSyncRequest(RpcMessage msg) throws TimeoutException;

    void sendAsyncRequest(Channel channel, RpcMessage msg);

    void sendAsyncRequest(RpcMessage msg);

    Object sendSyncRequest(Channel channel, RpcMessage msg) throws TimeoutException;
    void registerProcessor(final int messageType, final RemotingProcessor processor, final ExecutorService executor);
}
