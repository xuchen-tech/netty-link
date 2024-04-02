package com.fm.bingo;

import com.fm.bingo.util.NetUtil;
import io.netty.channel.Channel;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.net.InetSocketAddress;

public class NettyPooledFactory implements KeyedPooledObjectFactory<NettyPoolKey, Channel> {
    private final NettyClientBootstrap clientBootstrap;

    public NettyPooledFactory(NettyClientBootstrap clientBootstrap) {
        this.clientBootstrap = clientBootstrap;
    }

    @Override
    public void activateObject(NettyPoolKey nettyPoolKey, PooledObject<Channel> pooledObject) throws Exception {

    }

    @Override
    public void destroyObject(NettyPoolKey nettyPoolKey, PooledObject<Channel> pooledObject) throws Exception {
        if (pooledObject != null && pooledObject.getObject() != null) {
            pooledObject.getObject().disconnect();
            pooledObject.getObject().close();
        }
    }

    @Override
    public void destroyObject(NettyPoolKey key, PooledObject<Channel> p, DestroyMode destroyMode) throws Exception {
        KeyedPooledObjectFactory.super.destroyObject(key, p, destroyMode);
    }

    @Override
    public PooledObject<Channel> makeObject(NettyPoolKey nettyPoolKey) throws Exception {
        InetSocketAddress address = NetUtil.toInetSocketAddress(nettyPoolKey.getAddress());
        return new DefaultPooledObject<>(clientBootstrap.getNewChannel(address));
    }

    @Override
    public void passivateObject(NettyPoolKey nettyPoolKey, PooledObject<Channel> pooledObject) throws Exception {

    }


    @Override
    public boolean validateObject(NettyPoolKey nettyPoolKey, PooledObject<Channel> pooledObject) {
        return pooledObject != null && pooledObject.getObject().isActive();
    }
}
