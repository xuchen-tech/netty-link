package com.fm.bingo;

public class NettyPoolKey {
    private String address;
    public NettyPoolKey(String address) {
        this.address = address;
    }
    public String getAddress() {
        return address;
    }
    public NettyPoolKey setAddress(String address) {
        this.address = address;
        return this;
    }
}
