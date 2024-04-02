package com.fm.bingo;

import com.fm.bingo.util.CollectionUtils;
import io.netty.channel.Channel;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class NettyClientChannelManager {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNettyRemoting.class);
    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Short> servers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap = new ConcurrentHashMap<>();
    private final GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;
    NettyClientChannelManager(final NettyPooledFactory keyPooledFactory,
                              final NettyClientConfig clientConfig) {
        nettyClientKeyPool = new GenericKeyedObjectPool<>(keyPooledFactory);
        nettyClientKeyPool.setConfig(getNettyPoolConfig(clientConfig));
    }
    private GenericKeyedObjectPoolConfig getNettyPoolConfig(final NettyClientConfig clientConfig) {
        GenericKeyedObjectPoolConfig poolConfig = new GenericKeyedObjectPoolConfig();
        poolConfig.setMaxTotalPerKey(clientConfig.getMaxPoolActive());
        poolConfig.setMinIdlePerKey(clientConfig.getMinPoolIdle());
        poolConfig.setTestOnBorrow(clientConfig.isPoolTestBorrow());
        poolConfig.setTestOnReturn(clientConfig.isPoolTestReturn());
        poolConfig.setLifo(clientConfig.isPoolLifo());
        return poolConfig;
    }
    public Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        Channel channelFromPool = null;
        try {
            NettyPoolKey currentPoolKey = new NettyPoolKey(serverAddress);
            poolKeyMap.putIfAbsent(serverAddress, currentPoolKey);
            channelFromPool = nettyClientKeyPool.borrowObject(poolKeyMap.get(serverAddress));
            channels.put(serverAddress, channelFromPool);
        } catch (Exception exx) {
        }
        return channelFromPool;
    }
    void invalidateObject(final String serverAddress, final Channel channel) throws Exception {
        nettyClientKeyPool.invalidateObject(poolKeyMap.get(serverAddress), channel);
    }
    Channel acquireChannel(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (channelToServer != null) {
                return channelToServer;
            }
        }

        Object lockObj = CollectionUtils.computeIfAbsent(channelLocks, serverAddress, key -> new Object());
        synchronized (lockObj) {
            return doConnect(serverAddress);
        }
    }
    void destroyChannel(String serverAddress, Channel channel) {
        if (channel == null) {
            return;
        }
        try {
            if (channel.equals(channels.get(serverAddress))) {
                channels.remove(serverAddress);
            }
            nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
        } catch (Exception exx) {
            logger.error("return channel to rmPool error:{}", exx.getMessage());
        }
    }
    void reconnect() {
        List<String> availList;
        try {
            availList = getAvailServerList();
        } catch (Exception e) {
            logger.error("Failed to get available servers: {}", e.getMessage(), e);
            return;
        }
        Set<String> channelAddress = new HashSet<>(availList.size());
        for (String serverAddress : availList) {
            try {
                acquireChannel(serverAddress);
                channelAddress.add(serverAddress);
            } catch (Exception e) {
                logger.error("can not connect to {} cause:{}", serverAddress, e.getMessage(), e);
            }
        }
    }
    void releaseChannel(Channel channel, String serverAddress) {
        if (channel == null || serverAddress == null) {
            return;
        }
        try {
            synchronized (channelLocks.get(serverAddress)) {
                Channel ch = channels.get(serverAddress);
                if (ch == null) {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                    return;
                }
                if (ch.compareTo(channel) == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("return to pool, rm channel:{}", channel);
                    }
                    destroyChannel(serverAddress, channel);
                } else {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                }
            }
        } catch (Exception exx) {
            logger.error(exx.getMessage());
        }
    }
    private Channel getExistAliveChannel(Channel rmChannel, String serverAddress) {
        if (rmChannel.isActive()) {
            return rmChannel;
        } else {
            int i = 0;
            for (; i < NettyClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(NettyClientConfig.getCheckAliveInterval());
                } catch (InterruptedException exx) {
                    logger.error(exx.getMessage());
                }
                rmChannel = channels.get(serverAddress);
                if (rmChannel != null && rmChannel.isActive()) {
                    return rmChannel;
                }
            }
            if (i == NettyClientConfig.getMaxCheckAliveRetry()) {
                logger.warn("channel {} is not active after long wait, close it.", rmChannel);
                releaseChannel(rmChannel, serverAddress);
                return null;
            }
        }
        return null;
    }
    public List<String> getAvailServerList() {
        if (servers.isEmpty()) {
            return Collections.emptyList();
        }
        return servers.entrySet().parallelStream().map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
