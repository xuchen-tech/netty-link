package com.fm.bingo;

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.NettyRuntime;

public class NettyClientConfig {
    private static final int MAX_ALL_IDLE_SECONDS = 0;
    private static final int DEFAULT_WRITE_IDLE_SECONDS = 5;
    private static final int READIDLE_BASE_WRITEIDLE = 3;
    private static final int DEFAULT_MAX_POOL_ACTIVE = 10;
    private static final int DEFAULT_MIN_POOL_IDLE = 0;
    private static final long MAX_ACQUIRE_CONN_MILLS = 60 * 1000L;
    private static final boolean DEFAULT_POOL_TEST_BORROW = true;
    private static final boolean DEFAULT_POOL_TEST_RETURN = true;
    private static final boolean DEFAULT_POOL_LIFO = true;
    private static final String SOCKET_ADDRESS_START_CHAR = "/";
    private static final int MAX_NOT_WRITEABLE_RETRY = 2000;
    private static final int MAX_CHECK_ALIVE_RETRY = 300;
    private static final int CHECK_ALIVE_INTERVAL = 10;
    private static final String RPC_DISPATCH_THREAD_PREFIX = "rpcDispatch";
    private final int connectTimeoutMillis = 10000;
    private final int clientSocketSndBufSize = 153600;
    private final int clientSocketRcvBufSize = 153600;
    public static String getSocketAddressStartChar() {
        return SOCKET_ADDRESS_START_CHAR;
    }
    /**
     * Gets max not writeable retry.
     *
     * @return the max not writeable retry
     */
    public static int getMaxNotWriteableRetry() {
        return MAX_NOT_WRITEABLE_RETRY;
    }
    /**
     * Gets max check alive retry.
     *
     * @return the max check alive retry
     */
    public static int getMaxCheckAliveRetry() {
        return MAX_CHECK_ALIVE_RETRY;
    }
    /**
     * Gets check alive interval.
     *
     * @return the check alive interval
     */
    public static int getCheckAliveInterval() {
        return CHECK_ALIVE_INTERVAL;
    }
    /**
     * Gets connect timeout millis.
     *
     * @return the connect timeout millis
     */
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }
    /**
     * Gets client socket snd buf size.
     *
     * @return the client socket snd buf size
     */
    public int getClientSocketSndBufSize() {
        return clientSocketSndBufSize;
    }
    /**
     * Gets client socket rcv buf size.
     *
     * @return the client socket rcv buf size
     */
    public int getClientSocketRcvBufSize() {
        return clientSocketRcvBufSize;
    }
    /**
     * Gets client selector thread size
     *
     * @return the client selector thread size
     */
    public int getClientSelectorThreadSize() {
        // default client selector thread size
        return 1;
    }
    /**
     * Get client selector thread prefix string.
     *
     * @return the string
     */
    public String getClientSelectorThreadPrefix() {
        return "NettyClientSelector";
    }
    /**
     * Gets client worker threads.
     *
     * @return the client worker threads
     */
    public int getClientWorkerThreads() {
        return NettyRuntime.availableProcessors() * 2;
    }
    /**
     * Get client worker thread prefix string.
     *
     * @return the string
     */
    public String getClientWorkerThreadPrefix() {
        return "NettyClientWorkerThread";
    }
    /**
     * Gets client channel clazz.
     *
     * @return the client channel clazz
     */
    public Class<? extends Channel> getClientChannelClazz() {
        return NioSocketChannel.class;
    }
    /**
     * Gets channel max read idle seconds.
     *
     * @return the channel max read idle seconds
     */
    public int getChannelMaxReadIdleSeconds() {
        return DEFAULT_WRITE_IDLE_SECONDS * READIDLE_BASE_WRITEIDLE;
    }
    /**
     * Gets client channel max idle time seconds.
     *
     * @return the client channel max idle time seconds
     */
    public int getChannelMaxWriteIdleSeconds() {
        return DEFAULT_WRITE_IDLE_SECONDS;
    }
    /**
     * Gets channel max all idle seconds.
     *
     * @return the channel max all idle seconds
     */
    public int getChannelMaxAllIdleSeconds() {
        return MAX_ALL_IDLE_SECONDS;
    }
    /**
     * Gets max pool active.
     *
     * @return the max pool active
     */
    public int getMaxPoolActive() {
        return DEFAULT_MAX_POOL_ACTIVE;
    }
    /**
     * Gets min pool idle.
     *
     * @return the min pool idle
     */
    public int getMinPoolIdle() {
        return DEFAULT_MIN_POOL_IDLE;
    }
    /**
     * Get max acquire conn mills long.
     *
     * @return the long
     */
    public long getMaxAcquireConnMills() {
        return MAX_ACQUIRE_CONN_MILLS;
    }
    /**
     * Is pool test borrow boolean.
     *
     * @return the boolean
     */
    public boolean isPoolTestBorrow() {
        return DEFAULT_POOL_TEST_BORROW;
    }
    /**
     * Is pool test return boolean.
     *
     * @return the boolean
     */
    public boolean isPoolTestReturn() {
        return DEFAULT_POOL_TEST_RETURN;
    }
    /**
     * Is pool fifo boolean.
     *
     * @return the boolean
     */
    public boolean isPoolLifo() {
        return DEFAULT_POOL_LIFO;
    }
    /**
     * Gets compiler dispatch thread prefix.
     *
     * @return the compiler dispatch thread prefix
     */
    public String getCompilerDispatchThreadPrefix() {
        return RPC_DISPATCH_THREAD_PREFIX + "_" + "compiler";
    }

}
