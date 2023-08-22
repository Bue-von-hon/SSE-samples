package com.example.youthcon.preparation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TabIdToConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(TabIdToConnectionManager.class);

    // 탭과 연결된 커넥션 그리고 캐시를 이용한 만료처리
    // 타임 아웃시간이 지나도 탭을 커넥션을 들고있으면 안돼는데, 만료 시간을 설정하면 굳이 지우는 연산을 안해도된다.
    private final Cache<String, Connection> tabIdToConnection = CacheBuilder.newBuilder()
            .expireAfterAccess(300900L, TimeUnit.MILLISECONDS)
            .build();

    public Connection getIfPresent(final String tabId) {
        return tabIdToConnection.getIfPresent(tabId);
    }

    public void cleanUp() {
        tabIdToConnection.cleanUp();
    }

    public void put(final String tabId, final Connection newConnection) {
        tabIdToConnection.put(tabId, newConnection);
    }

    public long size() {
        return tabIdToConnection.size();
    }

    public void completeOldConnection(final String tabId) {
        final Connection oldConnection = tabIdToConnection.getIfPresent(tabId);

        if (oldConnection != null) {
            try {
                oldConnection.complete();
            } catch (Exception e) {
                log.info("exception : {}", e.getCause());
            }
        }
    }

    public void updateTabIdToConnection(final String tabId, final Connection newConnection) {
        tabIdToConnection.put(tabId, newConnection);
        log.info("emitter list size: {}", tabIdToConnection.size());
    }
}
