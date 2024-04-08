package com.fm.bingo;

import com.fm.bingo.common.NamedThreadFactory;
import com.fm.bingo.processor.ClientHeartbeatProcessor;
import com.fm.bingo.protocol.MessageType;
import io.netty.util.concurrent.EventExecutorGroup;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SampleNettyRemotingClient extends AbstractNettyRemotingClient {
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 20000;
    private static volatile SampleNettyRemotingClient instance;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private String applicationId;

    public SampleNettyRemotingClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup,
                                     ThreadPoolExecutor messageExecutor) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor);
    }

    public static SampleNettyRemotingClient getInstance(String applicationId) {
        SampleNettyRemotingClient sampleNettyRemotingClient = getInstance();
        sampleNettyRemotingClient.setApplicationId(applicationId);
        return sampleNettyRemotingClient;
    }

    public static SampleNettyRemotingClient getInstance() {
        if (instance == null) {
            synchronized (SampleNettyRemotingClient.class) {
                if (instance == null) {
                    NettyClientConfig nettyClientConfig = new NettyClientConfig();
                    final ThreadPoolExecutor messageExecutor = new ThreadPoolExecutor(
                            nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
                            KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                            new NamedThreadFactory("test",
                                    nettyClientConfig.getClientWorkerThreads()), new ThreadPoolExecutor.CallerRunsPolicy());
                    instance = new SampleNettyRemotingClient(nettyClientConfig, null, messageExecutor);
                }
            }
        }
        return instance;
    }

    @Override
    public void init() {
        registerProcessor();
        if (initialized.compareAndSet(false, true)) {
            super.init();
        }
        getClientChannelManager().reconnect();
    }

    @Override
    protected long getRpcRequestTimeout() {
        return Duration.ofSeconds(15).toMillis();
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    private void registerProcessor() {
        ClientHeartbeatProcessor clientHeartbeatProcessor = new ClientHeartbeatProcessor();
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, clientHeartbeatProcessor, null);
    }

    @Override
    public void destroy() {
        super.destroy();
        initialized.getAndSet(false);
        instance = null;
    }
}
