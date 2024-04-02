package com.fm.bingo.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetUtil {
    public static InetSocketAddress toInetSocketAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }
    public static String toStringAddress(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }
    public static String toStringAddress(SocketAddress address) {
        if (address == null) {
            return "";
        }
        return toStringAddress((InetSocketAddress) address);
    }
}
