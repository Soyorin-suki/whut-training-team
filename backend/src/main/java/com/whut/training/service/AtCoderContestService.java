package com.whut.training.service;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.UpcomingContestItem;
import com.whut.training.exception.BusinessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 AtCoder 官方近期比赛页读取并缓存比赛信息。
 *
 * <p>AtCoder 目前没有为近期比赛提供正式公开 JSON API，因此这里仅解析
 * 官方比赛页的 Upcoming Contests 表格，并在官方页面不可用时回退到最近一次缓存。
 */
@Service
@ServiceLog
public class AtCoderContestService {

    private static final String ATCODER_ORIGIN = "https://atcoder.jp";
    private static final DateTimeFormatter ATCODER_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxx");

    private final String contestsUrl;
    private final Duration cacheDuration;
    private volatile CacheSnapshot cacheSnapshot = new CacheSnapshot(List.of(), Instant.EPOCH);

    public AtCoderContestService(
            @Value("${atcoder.contests-url:https://atcoder.jp/contests/?lang=en}") String contestsUrl,
            @Value("${atcoder.cache-minutes:15}") long cacheMinutes
    ) {
        this.contestsUrl = contestsUrl;
        this.cacheDuration = Duration.ofMinutes(Math.max(1, cacheMinutes));
    }

    public List<UpcomingContestItem> getUpcomingContests() {
        Instant now = Instant.now();
        CacheSnapshot current = cacheSnapshot;
        if (now.isBefore(current.expiresAt())) {
            return current.items();
        }

        synchronized (this) {
            current = cacheSnapshot;
            if (now.isBefore(current.expiresAt())) {
                return current.items();
            }

            try {
                Document document = Jsoup.connect(contestsUrl)
                        .userAgent("WHUT-ACM Training Platform/1.0")
                        .timeout(8_000)
                        .get();
                List<UpcomingContestItem> items = parseUpcomingContests(document);
                cacheSnapshot = new CacheSnapshot(items, now.plus(cacheDuration));
                return items;
            } catch (Exception ex) {
                if (!current.items().isEmpty()) {
                    return current.items();
                }
                throw new BusinessException(503, "AtCoder 近期比赛暂时无法获取，请稍后重试");
            }
        }
    }

    List<UpcomingContestItem> parseUpcomingContests(Document document) {
        List<UpcomingContestItem> items = new ArrayList<>();
        for (Element row : document.select("#contest-table-upcoming tbody tr")) {
            List<Element> cells = row.select("td");
            Element contestLink = row.selectFirst("td:nth-child(2) a[href^='/contests/']");
            Element timeElement = row.selectFirst("time.fixtime-full");
            if (cells.size() < 4 || contestLink == null || timeElement == null) {
                continue;
            }

            String href = contestLink.attr("href");
            String contestId = href.substring(href.lastIndexOf('/') + 1);
            OffsetDateTime startTime = OffsetDateTime.parse(timeElement.text().trim(), ATCODER_TIME_FORMAT);
            String durationText = cells.get(2).text().trim();
            String type = readContestType(cells.get(1));

            items.add(new UpcomingContestItem(
                    "ATCODER",
                    contestId,
                    contestLink.text().trim(),
                    type,
                    startTime.toString(),
                    parseDurationMinutes(durationText),
                    cells.get(3).text().trim(),
                    ATCODER_ORIGIN + href
            ));
        }
        return List.copyOf(items);
    }

    private String readContestType(Element contestCell) {
        Element typeMarker = contestCell.selectFirst("span[title]");
        return typeMarker == null ? "Contest" : typeMarker.attr("title");
    }

    private int parseDurationMinutes(String durationText) {
        String[] parts = durationText.split(":");
        if (parts.length != 2) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record CacheSnapshot(List<UpcomingContestItem> items, Instant expiresAt) {
    }
}
