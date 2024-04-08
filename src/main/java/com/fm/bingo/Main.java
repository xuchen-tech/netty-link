package com.fm.bingo;

import com.fm.bingo.protocol.MessageType;
import com.fm.bingo.protocol.RpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static int requestId = 100;

    public static void main(String[] args) throws InterruptedException, TimeoutException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8083);
        logger.info("add remote server: {}", inetSocketAddress);
        SampleNettyRemotingClient.getInstance().addRemoteServer(inetSocketAddress);
        SampleNettyRemotingClient.getInstance().init();
        while (true) {
            Thread.sleep(1000);
            RpcMessage testAsyncMessage = new RpcMessage();
            testAsyncMessage.setRequestId(requestId++);
            testAsyncMessage.setType(MessageType.TYPE_ASYNC_MSG);
            String payloadString = "test";
            testAsyncMessage.setPayload(payloadString.getBytes(StandardCharsets.UTF_8));
            testAsyncMessage.setLength(payloadString.getBytes(StandardCharsets.UTF_8).length);
            SampleNettyRemotingClient.getInstance().sendSyncRequest(testAsyncMessage);
        }
    }
}