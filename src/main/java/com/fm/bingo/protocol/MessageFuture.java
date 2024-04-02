package com.fm.bingo.protocol;

import com.fm.bingo.common.ShouldNeverHappenException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MessageFuture {
    private final long start = System.currentTimeMillis();
    private final CompletableFuture<Object> origin = new CompletableFuture<>();
    private RpcMessage requestMessage;
    private long timeout;
    public boolean isTimeout() {
        return System.currentTimeMillis() - start > timeout;
    }
    public void setResultMessage(Object obj) {
        origin.complete(obj);
    }
    public RpcMessage getRequestMessage() {
        return requestMessage;
    }
    public void setRequestMessage(RpcMessage requestMessage) {
        this.requestMessage = requestMessage;
    }
    public long getTimeout() {
        return timeout;
    }
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
    public Object get(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        Object result = null;
        try {
            result = origin.get(timeout, unit);
            if (result instanceof TimeoutException) {
                throw (TimeoutException)result;
            }
        } catch (ExecutionException e) {
            throw new ShouldNeverHappenException("Should not get results in a multi-threaded environment", e);
        } catch (TimeoutException e) {
            throw new TimeoutException(String.format("%s ,cost: %d ms", e.getMessage(), System.currentTimeMillis() - start));
        }
        return result;
    }
}
