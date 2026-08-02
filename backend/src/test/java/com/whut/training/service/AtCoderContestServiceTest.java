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
                createService();
        var items = service.parseUpcomingContests(Jsoup.parse(html));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).contestId()).isEqualTo("abc469");
        assertThat(items.get(0).startTime()).isEqualTo("2026-08-01T21:00+09:00");
        assertThat(items.get(0).durationMinutes()).isEqualTo(100);
        assertThat(items.get(0).url()).isEqualTo("https://atcoder.jp/contests/abc469");
        assertThat(items.get(1).type()).isEqualTo("Heuristic");
        assertThat(items.get(1).durationMinutes()).isEqualTo(14_400);
    }

    @Test
    void parsesUpcomingCodeforcesContests() {
        String json = """
                {"status":"OK","result":[
                  {"id":2200,"name":"Codeforces Round","type":"CF","phase":"BEFORE","durationSeconds":7200,"startTimeSeconds":1785704400},
                  {"id":2199,"name":"Finished Round","type":"CF","phase":"FINISHED","durationSeconds":7200,"startTimeSeconds":1780000000}
                ]}
                """;

        var items = createService().parseCodeforcesContests(json);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).platform()).isEqualTo("CODEFORCES");
        assertThat(items.get(0).contestId()).isEqualTo("2200");
        assertThat(items.get(0).durationMinutes()).isEqualTo(120);
    }

    @Test
    void parsesUpcomingNowcoderCards() {
        String html = """
                <div class="platform-item">
                  <h4><a href="/acm/contest/138240">牛客周赛 Round 155</a><span class="tag-rating">Rated</span></h4>
                  <li class="match-time-icon">比赛时间： 2026-08-02 19:00 至 2026-08-02 21:00 (时长:2小时)</li>
                  <li class="icon-nc-flash2">不计Rating的范围：Rating＞1599</li>
                </div>
                """;

        var items = createService().parseNowcoderContests(
                Jsoup.parse(html), java.time.Instant.parse("2026-08-01T00:00:00Z"));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).platform()).isEqualTo("NOWCODER");
        assertThat(items.get(0).durationMinutes()).isEqualTo(120);
        assertThat(items.get(0).url()).isEqualTo("https://ac.nowcoder.com/acm/contest/138240");
    }

    @Test
    void parsesUpcomingLuoguContext() {
        String html = """
                <script id="lentille-context" type="application/json">
                  {"data":{"contests":{"result":[
                    {"id":341590,"startTime":1786861800,"endTime":1786869000,"name":"洛谷月赛","method":2,"rated":1}
                  ]}}}
                </script>
                """;

        var items = createService().parseLuoguContests(
                Jsoup.parse(html), java.time.Instant.parse("2026-08-01T00:00:00Z"));

        assertThat(items).hasSize(1);
        assertThat(items.get(0).platform()).isEqualTo("LUOGU");
        assertThat(items.get(0).type()).isEqualTo("ICPC");
        assertThat(items.get(0).durationMinutes()).isEqualTo(120);
    }

    private AtCoderContestService createService() {
        return new AtCoderContestService(
                "https://atcoder.jp/contests/?lang=en",
                15,
                "https://codeforces.com/api/contest.list?gym=false",
                "https://ac.nowcoder.com/acm/contest/vip-index",
                "https://www.luogu.com.cn/contest/list"
        );
    }
}
