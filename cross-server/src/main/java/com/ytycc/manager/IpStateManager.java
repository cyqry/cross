package com.ytycc.manager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class IpStateManager {
    private final ConcurrentMap<String, IpState> ipStateMap = new ConcurrentHashMap<>();

    public IpState getIpState(String ip) {
        return ipStateMap.computeIfAbsent(ip, k -> new IpState());
    }

    public void removeConnectionState(String ip) {
        ipStateMap.remove(ip);
    }

    // 定义连接状态信息，包括连接状态和校验错误次数
    public static class IpState {
        private boolean hasConnection = true;
        private int errorCount = 0;

        public boolean hasConnection() {
            return hasConnection;
        }

        public void hasConnection(boolean connected) {
            this.hasConnection = connected;
        }

        public int errorCount() {
            return errorCount;
        }

        public void incrementErrorCount() {
            this.errorCount++;
        }

        public void resetErrorCount() {
            this.errorCount = 0;
        }
    }
}
