package com.inboxintelligence.ingester.external;


import com.google.api.services.gmail.model.History;
import com.inboxintelligence.ingester.internal.GmailClientFactory;
import com.inboxintelligence.ingester.model.GmailMailbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GmailSyncService {

    private final GmailClientFactory gmailClientFactory;


    public void triggerSyncJob(GmailMailbox gmailMailbox) throws IOException {

        log.info("Triggering Sync Job: {}", gmailMailbox.getEmailAddress());

        String pageToken = null;
        Long latestHistoryId = null;

        var gmail = gmailClientFactory.createUsingRefreshToken(gmailMailbox.getRefreshToken());

        do {
            var request = gmail.users()
                    .history()
                    .list("me")
                    .setStartHistoryId(BigInteger.valueOf(gmailMailbox.getHistoryId()))
                    .setPageToken(pageToken);

            var response = request.execute();

            if (response == null) {
                break;
            }

            this.processListHistoryResponse(response.getHistory());

            pageToken = response.getNextPageToken();
            latestHistoryId = response.getHistoryId().longValue();
            log.info("Setting latest history Id {}", latestHistoryId);

        } while (pageToken != null);
    }

    private void processListHistoryResponse(List<History> history) {
        //TODO
    }
}
