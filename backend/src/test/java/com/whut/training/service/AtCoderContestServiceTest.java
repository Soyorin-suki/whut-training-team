package com.whut.training.service;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtCoderContestServiceTest {

    @Test
    void parsesUpcomingContestTableFromOfficialPageShape() {
        String html = """
                <div id="contest-table-upcoming">
                  <table>
                    <tbody>
                      <tr>
                        <td><time class="fixtime fixtime-full">2026-08-01 21:00:00+0900</time></td>
                        <td>
                          <span title="Algorithm">A</span>
                          <a href="/contests/abc469">AtCoder Beginner Contest 469</a>
                        </td>
                        <td>01:40</td>
                        <td>- 1999</td>
                      </tr>
                      <tr>
                        <td><time class="fixtime fixtime-full">2026-08-29 15:00:00+0900</time></td>
                        <td>
                          <span title="Heuristic">H</span>
                          <a href="/contests/ahc070">AtCoder Heuristic Contest 070</a>
                        </td>
                        <td>240:00</td>
                        <td>All</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                """;

        AtCoderContestService service =
                new AtCoderContestService("https://atcoder.jp/contests/?lang=en", 15);
        var items = service.parseUpcomingContests(Jsoup.parse(html));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).contestId()).isEqualTo("abc469");
        assertThat(items.get(0).startTime()).isEqualTo("2026-08-01T21:00+09:00");
        assertThat(items.get(0).durationMinutes()).isEqualTo(100);
        assertThat(items.get(0).url()).isEqualTo("https://atcoder.jp/contests/abc469");
        assertThat(items.get(1).type()).isEqualTo("Heuristic");
        assertThat(items.get(1).durationMinutes()).isEqualTo(14_400);
    }
}
