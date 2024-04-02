package com.fm.bingo;

import io.netty.channel.Channel;

import java.util.concurrent.ExecutorService;

public interface RemotingClient {
    void sendAsyncRequest(Channel channel, Object msg);
    void sendSyncRequest(Channel channel, Object msg);
    void registerProcessor(final int messageType, final RemotingProcessor processor, final ExecutorService executor);
}
