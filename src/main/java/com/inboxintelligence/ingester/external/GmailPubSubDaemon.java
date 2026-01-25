package com.inboxintelligence.ingester.external;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailPubSubDaemon {

    private volatile boolean running = true;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(createThreadFactory());
    private final GmailPubSubPoller gmailPubSubPoller;

    private static ThreadFactory createThreadFactory() {
        return r -> {
            Thread t = new Thread(r, "pubsub-worker");
            t.setDaemon(true);
            return t;
        };
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PostConstruct
    public void start() {
        executor.submit(() -> {
            while (running) {
                try {
                    boolean workDone = gmailPubSubPoller.poll();
                    if (!workDone) {
                        sleep(2000);
                    }
                } catch (Exception e) {
                    log.error("Error while polling Gmail Pub/Sub", e);
                    sleep(5000);
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
