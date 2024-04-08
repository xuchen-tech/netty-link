package com.fm.bingo;

import com.fm.bingo.common.FrameworkException;
import com.fm.bingo.common.NamedThreadFactory;
import com.fm.bingo.common.Pair;
import com.fm.bingo.protocol.MessageFuture;
import com.fm.bingo.protocol.RpcMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public abstract class AbstractNettyRemoting implements Disposable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNettyRemoting.class);
    private static final long NOT_WRITEABLE_CHECK_MILLS = 10L;
    private static final int TIMEOUT_CHECK_INTERVAL = 3000;
    protected final ThreadPoolExecutor messageExecutor;
    protected final ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("timeoutChecker", 1, true));
    protected final ConcurrentHashMap<Integer, MessageFuture> futures = new ConcurrentHashMap<>();
    protected final Object lock = new Object();
    protected final HashMap<Integer/*MessageType*/, Pair<RemotingProcessor, ExecutorService>> processorTable = new HashMap<>(32);
    protected volatile long nowMills = 0;
    public AbstractNettyRemoting(ThreadPoolExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
    }
    public void init() {
        timerExecutor.scheduleAtFixedRate(() -> {
            for (Map.Entry<Integer, MessageFuture> entry : futures.entrySet()) {
                MessageFuture future = entry.getValue();
                if (future.isTimeout()) {
                    futures.remove(entry.getKey());
                    RpcMessage rpcMessage = future.getRequestMessage();
                    future.setResultMessage(new TimeoutException(String
                            .format("msgId: %s ,msgType: %s ,msg: %s ,request timeout", rpcMessage.getRequestId(), String.valueOf(rpcMessage.getType()), rpcMessage.getPayload())));
                }
            }

            nowMills = System.currentTimeMillis();
        }, TIMEOUT_CHECK_INTERVAL, TIMEOUT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }
    public void destroyChannel(Channel channel) {
        destroyChannel(getAddressFromChannel(channel), channel);
    }
    protected String getAddressFromContext(ChannelHandlerContext ctx) {
        return getAddressFromChannel(ctx.channel());
    }
    protected String getAddressFromChannel(Channel channel) {
        SocketAddress socketAddress = channel.remoteAddress();
        String address = socketAddress.toString();
        if (socketAddress.toString().indexOf(NettyClientConfig.getSocketAddressStartChar()) == 0) {
            address = socketAddress.toString().substring(NettyClientConfig.getSocketAddressStartChar().length());
        }
        return address;
    }
    private void channelWritableCheck(Channel channel, Object msg) {
        int tryTimes = 0;
        synchronized (lock) {
            while (!channel.isWritable()) {
                try {
                    tryTimes++;
                    if (tryTimes > NettyClientConfig.getMaxNotWriteableRetry()) {
                        destroyChannel(channel);
//                        throw new FrameworkException("msg:" + ((msg == null) ? "null" : msg.toString()),
//                                FrameworkErrorCode.ChannelIsNotWritable);
                    }
                    lock.wait(NOT_WRITEABLE_CHECK_MILLS);
                } catch (InterruptedException exx) {
                    logger.error(exx.getMessage());
                }
            }
        }
    }
    protected void processMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        final Pair<RemotingProcessor, ExecutorService> pair = this.processorTable.get(rpcMessage.getType());
        if (futures.containsKey(rpcMessage.getRequestId())) {
            MessageFuture messageFuture = futures.remove(rpcMessage.getRequestId());
            messageFuture.setResultMessage(rpcMessage);
        }
        if (pair != null) {
            if (pair.getSecond() != null) {
                try {
                    pair.getSecond().execute(() -> {
                        try {
                            pair.getFirst().process(ctx, rpcMessage);
                        } catch (Throwable th) {
                            logger.error(th.getMessage(), th);
                        } finally {
                            MDC.clear();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    logger.error("thread pool is full, current max pool size is " + messageExecutor.getActiveCount());
                }
            } else {
                try {
                    pair.getFirst().process(ctx, rpcMessage);
                } catch (Throwable th) {
                    logger.error(th.getMessage(), th);
                }
            }
        } else {
            logger.error("This message type [{}] has no processor.");
        }
    }
    public abstract void destroyChannel(String serverAddress, Channel channel);

    @Override
    public void destroy() {
        timerExecutor.shutdown();
        messageExecutor.shutdown();
    }

    protected void sendAsync(Channel channel, RpcMessage rpcMessage) {
        channelWritableCheck(channel, rpcMessage.getPayload());
        if (logger.isDebugEnabled()) {
            logger.debug("write message:" + rpcMessage.getPayload() + ", channel:" + channel + ",active?"
                    + channel.isActive() + ",writable?" + channel.isWritable() + ",isopen?" + channel.isOpen());
        }

        channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                destroyChannel(future.channel());
            }
        });
    }

    protected Object sendSync(Channel channel, RpcMessage rpcMessage, long timeoutMillis) throws TimeoutException {
        if (timeoutMillis <= 0) {
            throw new FrameworkException("timeout should more than 0ms");
        }
        if (channel == null) {
            logger.warn("sendSync nothing, caused by null channel.");
            return null;
        }

        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(timeoutMillis);
        futures.put(rpcMessage.getRequestId(), messageFuture);

        channelWritableCheck(channel, rpcMessage.getPayload());

        channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                MessageFuture messageFuture1 = futures.remove(rpcMessage.getRequestId());
                if (messageFuture1 != null) {
                    messageFuture1.setResultMessage(future.cause());
                }
                destroyChannel(future.channel());
            }
        });

        try {
            Object result = messageFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return result;
        } catch (Exception exx) {
            logger.error("wait response error:{},ip:{},request:{}", exx.getMessage(), channel.remoteAddress(),
                    rpcMessage.getPayload());
            if (exx instanceof TimeoutException) {
                throw (TimeoutException) exx;
            } else {
                throw new RuntimeException(exx);
            }
        }
    }
}
