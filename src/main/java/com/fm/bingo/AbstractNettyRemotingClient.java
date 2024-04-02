package com.fm.bingo;

import com.fm.bingo.common.Pair;
import com.fm.bingo.protocol.RpcMessage;
import com.fm.bingo.util.NetUtil;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AbstractNettyRemotingClient extends AbstractNettyRemoting implements RemotingClient {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNettyRemoting.class);
    private static final long SCHEDULE_DELAY_MILLS = 60 * 1000L;
    private static final long SCHEDULE_INTERVAL_MILLS = 10 * 1000L;
    private final NettyClientBootstrap clientBootstrap;
    private final NettyClientChannelManager clientChannelManager;
    public AbstractNettyRemotingClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup,
                                       ThreadPoolExecutor messageExecutor) {
        super(messageExecutor);
        clientBootstrap = new NettyClientBootstrap(nettyClientConfig, eventExecutorGroup);
        clientBootstrap.setChannelHandlers(new ClientHandler());
        clientChannelManager = new NettyClientChannelManager(
                new NettyPooledFactory(clientBootstrap), nettyClientConfig);
    }
    @Override
    public void init() {
        timerExecutor.scheduleAtFixedRate(() -> clientChannelManager.reconnect(), SCHEDULE_DELAY_MILLS, SCHEDULE_INTERVAL_MILLS, TimeUnit.MILLISECONDS);
        super.init();
        clientBootstrap.start();
    }
    @Override
    public void registerProcessor(int requestCode, RemotingProcessor processor, ExecutorService executor) {
        Pair<RemotingProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(requestCode, pair);
    }
    public NettyClientChannelManager getClientChannelManager() {
        return clientChannelManager;
    }
    @Override
    public void destroyChannel(String serverAddress, Channel channel) {

    }

    @Override
    public void sendAsyncRequest(Channel channel, Object msg) {

    }
    @Override
    public void sendSyncRequest(Channel channel, Object msg) {

    }

    @ChannelHandler.Sharable
    class ClientHandler extends ChannelDuplexHandler {

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof RpcMessage)) {
                return;
            }
            processMessage(ctx, (RpcMessage) msg);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            synchronized (lock) {
                if (ctx.channel().isWritable()) {
                    lock.notifyAll();
                }
            }
            ctx.fireChannelWritabilityChanged();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (messageExecutor.isShutdown()) {
                return;
            }

            logger.info("channel inactive: {}", ctx.channel());

            clientChannelManager.releaseChannel(ctx.channel(), NetUtil.toStringAddress(ctx.channel().remoteAddress()));
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
                if (idleStateEvent.state() == IdleState.READER_IDLE) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("channel {} read idle.", ctx.channel());
                    }
                    try {
                        String serverAddress = NetUtil.toStringAddress(ctx.channel().remoteAddress());
                        clientChannelManager.invalidateObject(serverAddress, ctx.channel());
                    } catch (Exception exx) {
                        logger.error(exx.getMessage());
                    } finally {
                        clientChannelManager.releaseChannel(ctx.channel(), getAddressFromContext(ctx));
                    }
                }
                if (idleStateEvent == IdleStateEvent.WRITER_IDLE_STATE_EVENT) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("will send ping msg,channel {}", ctx.channel());
                        }
                        // TODO send ping message
//                        AbstractNettyRemotingClient.this.sendAsyncRequest(ctx.channel(), new HeartbeatMessage());
                    } catch (Throwable throwable) {
                        logger.error("send request error: {}", throwable.getMessage(), throwable);
                    }
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.error(NetUtil.toStringAddress(ctx.channel().remoteAddress()) + "connect exception. " + cause.getMessage(), cause);
            clientChannelManager.releaseChannel(ctx.channel(), getAddressFromChannel(ctx.channel()));
            if (logger.isDebugEnabled()) {
                logger.debug("remove exception rm channel:{}", ctx.channel());
            }
            super.exceptionCaught(ctx, cause);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
            super.close(ctx, future);
        }
    }
}
