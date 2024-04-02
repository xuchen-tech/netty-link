package com.fm.bingo;

import com.fm.bingo.common.NamedThreadFactory;
import com.fm.bingo.protocol.RpcMessageDecoder;
import com.fm.bingo.protocol.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class NettyClientBootstrap implements RemotingBootstrap {
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    private final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker;
    private EventExecutorGroup defaultEventExecutorGroup;
    private ChannelHandler[] channelHandlers;
    public NettyClientBootstrap(NettyClientConfig nettyClientConfig,
                                EventExecutorGroup eventExecutorGroup) {
        if (nettyClientConfig == null) {
            nettyClientConfig = new NettyClientConfig();
        }
        this.nettyClientConfig = nettyClientConfig;
        int selectorThreadSize = this.nettyClientConfig.getClientSelectorThreadSize();
        this.eventLoopGroupWorker = new NioEventLoopGroup(selectorThreadSize,
                new NamedThreadFactory(getThreadPrefix(this.nettyClientConfig.getClientSelectorThreadPrefix()),
                        selectorThreadSize));
        this.defaultEventExecutorGroup = eventExecutorGroup;
    }
    protected void setChannelHandlers(final ChannelHandler... handlers) {
        if (handlers != null) {
            channelHandlers = handlers;
        }
    }
    private void addChannelPipelineLast(Channel channel, ChannelHandler... handlers) {
        if (channel != null && handlers != null) {
            channel.pipeline().addLast(handlers);
        }
    }
    @Override
    public void start() {
        if (this.defaultEventExecutorGroup == null) {
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                    new NamedThreadFactory(getThreadPrefix(nettyClientConfig.getClientWorkerThreadPrefix()),
                            nettyClientConfig.getClientWorkerThreads()));
        }
        this.bootstrap.group(this.eventLoopGroupWorker).channel(
                nettyClientConfig.getClientChannelClazz()).option(
                ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()).option(
                ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()).option(ChannelOption.SO_RCVBUF,
                nettyClientConfig.getClientSocketRcvBufSize());
        bootstrap.handler(
                new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                                        new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                                nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                                nettyClientConfig.getChannelMaxAllIdleSeconds()))
                                .addLast(new RpcMessageDecoder())
                                .addLast(new RpcMessageEncoder());
                        if (channelHandlers != null) {
                            addChannelPipelineLast(ch, channelHandlers);
                        }
                    }
                });
    }

    public Channel getNewChannel(InetSocketAddress address) {
        Channel channel = null;
        ChannelFuture f = this.bootstrap.connect(address);
        try {
            f.await(this.nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (f.isCancelled()) {

            } else if (!f.isSuccess()) {

            } else {
                channel = f.channel();
            }
        } catch (Exception e) {

        }
        return channel;
    }

    @Override
    public void shutdown() {
        this.eventLoopGroupWorker.shutdownGracefully();
        if (this.defaultEventExecutorGroup != null) {
            this.defaultEventExecutorGroup.shutdownGracefully();
        }
    }

    private String getThreadPrefix(String threadPrefix) {
        return threadPrefix + THREAD_PREFIX_SPLIT_CHAR + "compiler";
    }
}
