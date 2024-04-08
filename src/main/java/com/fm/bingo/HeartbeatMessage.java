package com.fm.bingo;

import com.fm.bingo.protocol.MessageType;
import com.fm.bingo.protocol.RpcMessage;

public class HeartbeatMessage extends RpcMessage {
    public HeartbeatMessage() {
        this.setRequestId(0);
        this.setType(MessageType.TYPE_HEARTBEAT_MSG);
        this.setLength(0);
    }
}
