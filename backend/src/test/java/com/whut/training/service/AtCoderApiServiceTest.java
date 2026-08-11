package com.whut.training.service;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AtCoderApiServiceTest {
    private final AtCoderApiService service = new AtCoderApiService(
            "https://atcoder.jp/", "https://example.test/submissions"
    );

    @Test
    void readsAffiliationUsedByAccountBinding() {
        var document = Jsoup.parse("""
                <table class="dl-table"><tbody>
                  <tr><th>Country/Region</th><td>China</td></tr>
                  <tr><th>Affiliation</th><td>WHUT-ACM-7A2B9C1D</td></tr>
                </tbody></table>
                """);

        assertThat(service.readAffiliation(document)).isEqualTo("WHUT-ACM-7A2B9C1D");
        assertThat(service.profileUrl("tourist")).isEqualTo("https://atcoder.jp/users/tourist");
    }

    @Test
    void parsesRatedAndUnratedOfficialHistoryEntries() {
        var history = service.parseHistory("""
                [{
                  "ContestScreenName":"abc460.contest.atcoder.jp",
                  "ContestName":"AtCoder Beginner Contest 460",
                  "IsRated":true,
                  "Place":321,
                  "Performance":1800,
                  "OldRating":1700,
                  "NewRating":1750,
                  "EndTime":"2026-08-01T23:00:00+08:00"
                },{
                  "ContestScreenName":"abc459.contest.atcoder.jp",
                  "ContestName":"AtCoder Beginner Contest 459",
                  "IsRated":false,
                  "Place":99,
                  "Performance":null,
                  "OldRating":null,
                  "NewRating":null,
                  "EndTime":"2026-07-25T23:00:00+08:00"
                }]
                """);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).contestId()).isEqualTo("abc460");
        assertThat(history.get(0).rated()).isTrue();
        assertThat(history.get(0).newRating()).isEqualTo(1750);
        assertThat(history.get(1).rated()).isFalse();
        assertThat(history.get(1).performance()).isNull();
    }

    @Test
    void countsOnlyUniqueAcceptedProblemsInsideOfficialContestWindow() {
        var accepted = service.parseAcceptedProblems("""
                [
                  {"epoch_second":1010,"contest_id":"abc460","problem_id":"abc460_a","result":"AC"},
                  {"epoch_second":1020,"contest_id":"abc460","problem_id":"abc460_a","result":"AC"},
                  {"epoch_second":1030,"contest_id":"abc460","problem_id":"abc460_b","result":"WA"},
                  {"epoch_second":1040,"contest_id":"abc459","problem_id":"abc459_a","result":"AC"},
                  {"epoch_second":1201,"contest_id":"abc460","problem_id":"abc460_c","result":"AC"}
                ]
                """, "abc460", 1000, 1200);

        assertThat(accepted.count()).isEqualTo(1);
        assertThat(accepted.problemIds()).containsExactly("abc460_a");
    }
}
