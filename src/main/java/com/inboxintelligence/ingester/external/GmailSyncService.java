package com.inboxintelligence.ingester.external;


import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.HistoryMessageDeleted;
import com.inboxintelligence.ingester.internal.GmailClientFactory;
import com.inboxintelligence.ingester.model.GmailMailbox;
import com.inboxintelligence.ingester.service.GmailMailboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GmailSyncService {

    private final GmailClientFactory gmailClientFactory;
    private final GmailMailboxService gmailMailboxService;


    public void triggerSyncJob(GmailMailbox gmailMailbox) throws IOException {

        log.info("Triggering Sync Job: {}", gmailMailbox.getEmailAddress());

        Long latestHistoryId = null;
        String pageToken = null;
        var gmail = gmailClientFactory.createUsingRefreshToken(gmailMailbox.getRefreshToken());

        do {
            var request = gmail.users()
                    .history()
                    .list("me")
                    .setStartHistoryId(BigInteger.valueOf(gmailMailbox.getHistoryId()))
                    .setPageToken(pageToken);

            var response = request.execute();

            if (response == null) {
                log.error("Stopping: Gmail returned null response");
                break;
            }

            processListHistoryResponse(response.getHistory());
            pageToken = response.getNextPageToken();
            latestHistoryId = response.getHistoryId().longValue();

        } while (pageToken != null);

        if (latestHistoryId != null) {

            log.info("Setting latest history Id {}", latestHistoryId);
            gmailMailbox.setHistoryId(latestHistoryId);
            gmailMailboxService.save(gmailMailbox);

        }
    }

    private void processListHistoryResponse(List<History> historyList) {

        if (CollectionUtils.isEmpty(historyList)) {
            log.error("Stopping: Gmail returned empty historyList");
            return;
        }

        for (History history : historyList) {

            if (history == null
                    || CollectionUtils.isEmpty(history.getMessagesAdded())
                    || CollectionUtils.isEmpty(history.getMessagesDeleted())) {
                continue;
            }

            history.getMessagesAdded().forEach(this::processNewMessageAdded);
            history.getMessagesDeleted().forEach(this::processMessagesDeleted);
        }
    }

    private void processNewMessageAdded(HistoryMessageAdded historyMessageAdded) {
        //Add code
    }

    private void processMessagesDeleted(HistoryMessageDeleted historyMessageDeleted) {
        //add Code

    }
}
