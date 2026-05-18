package com.whut.training.controller;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class RankControllerIntegrationTest {

    private static final String TEST_DB_NAME = "whut-training-rank-test-" + System.nanoTime();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + TEST_DB_NAME + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenSessionRepository authTokenSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM auth_token_session");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void returnsPagedLeaderboardAndCurrentUserRank() throws Exception {
        User carol = createUser("carol", 50);
        createUser("alice", 30);
        User bob = createUser("bob", 30);

        String accessToken = "access-bob";
        String refreshToken = "refresh-bob";
        authTokenSessionRepository.save(new AuthTokenSession(
                bob.getId(),
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(3600).getEpochSecond(),
                Instant.now().plusSeconds(7200).getEpochSecond()
        ));

        mockMvc.perform(get("/api/rankings")
                        .param("type", "DAILY_TOTAL")
                        .param("page", "1")
                        .param("pageSize", "2")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.entries[0].username").value("carol"))
                .andExpect(jsonPath("$.data.entries[0].score").value(50))
                .andExpect(jsonPath("$.data.entries[1].username").value("alice"))
                .andExpect(jsonPath("$.data.entries[1].rank").value(2))
                .andExpect(jsonPath("$.data.currentUserEntry.username").value("bob"))
                .andExpect(jsonPath("$.data.currentUserEntry.rank").value(3));

        mockMvc.perform(get("/api/rankings/me")
                        .param("type", "DAILY_TOTAL")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("bob"))
                .andExpect(jsonPath("$.data.rank").value(3))
                .andExpect(jsonPath("$.data.score").value(30));
    }

    @Test
    void rejectsUnsupportedType() throws Exception {
        User user = createUser("alice", 10);
        String accessToken = "access-alice";
        String refreshToken = "refresh-alice";
        authTokenSessionRepository.save(new AuthTokenSession(
                user.getId(),
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(3600).getEpochSecond(),
                Instant.now().plusSeconds(7200).getEpochSecond()
        ));

        mockMvc.perform(get("/api/rankings")
                        .param("type", "DAILY_7D")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("unsupported leaderboard type"));
    }

    @Test
    void rejectsUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/rankings").param("type", "DAILY_TOTAL"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void returnsSolvedCountAndHardSolvedCountLeaderboards() throws Exception {
        User alice = createUser("alice", 10);
        User bob = createUser("bob", 20);
        User carol = createUser("carol", 30);

        userRepository.updateUserSolvedProblemStats(alice.getId(), 120, 8, 0, 0, 0);
        userRepository.updateUserSolvedProblemStats(bob.getId(), 120, 5, 0, 0, 0);
        userRepository.updateUserSolvedProblemStats(carol.getId(), 80, 12, 0, 0, 0);

        String accessToken = "access-bob";
        String refreshToken = "refresh-bob";
        authTokenSessionRepository.save(new AuthTokenSession(
                bob.getId(),
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(3600).getEpochSecond(),
                Instant.now().plusSeconds(7200).getEpochSecond()
        ));

        mockMvc.perform(get("/api/rankings")
                        .param("type", "SOLVED_COUNT")
                        .param("page", "1")
                        .param("pageSize", "3")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.data.entries[0].score").value(120))
                .andExpect(jsonPath("$.data.entries[1].username").value("bob"))
                .andExpect(jsonPath("$.data.entries[1].score").value(120))
                .andExpect(jsonPath("$.data.entries[1].rank").value(2))
                .andExpect(jsonPath("$.data.currentUserEntry.username").value("bob"))
                .andExpect(jsonPath("$.data.currentUserEntry.rank").value(2));

        mockMvc.perform(get("/api/rankings/me")
                        .param("type", "HARD_SOLVED_COUNT")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("bob"))
                .andExpect(jsonPath("$.data.rank").value(3))
                .andExpect(jsonPath("$.data.score").value(5));
    }

    @Test
    void returnsCurrentAndLongestStreakLeaderboards() throws Exception {
        User alice = createUser("alice", 10);
        User bob = createUser("bob", 20);
        User carol = createUser("carol", 30);

        userRepository.updateUserStreakStats(alice.getId(), 5, 7);
        userRepository.updateUserStreakStats(bob.getId(), 5, 6);
        userRepository.updateUserStreakStats(carol.getId(), 2, 9);

        String accessToken = "access-bob";
        String refreshToken = "refresh-bob";
        authTokenSessionRepository.save(new AuthTokenSession(
                bob.getId(),
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(3600).getEpochSecond(),
                Instant.now().plusSeconds(7200).getEpochSecond()
        ));

        mockMvc.perform(get("/api/rankings")
                        .param("type", "CURRENT_STREAK")
                        .param("page", "1")
                        .param("pageSize", "3")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.data.entries[0].score").value(5))
                .andExpect(jsonPath("$.data.entries[1].username").value("bob"))
                .andExpect(jsonPath("$.data.entries[1].score").value(5))
                .andExpect(jsonPath("$.data.entries[1].rank").value(2))
                .andExpect(jsonPath("$.data.currentUserEntry.username").value("bob"))
                .andExpect(jsonPath("$.data.currentUserEntry.rank").value(2))
                .andExpect(jsonPath("$.data.currentUserEntry.score").value(5));

        mockMvc.perform(get("/api/rankings/me")
                        .param("type", "LONGEST_STREAK")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("bob"))
                .andExpect(jsonPath("$.data.rank").value(3))
                .andExpect(jsonPath("$.data.score").value(6));
    }

    private User createUser(String username, int score) {
        User user = new User(
                null,
                username,
                username + "@example.com",
                "password123",
                UserRole.USER
        );
        userRepository.save(user);
        userRepository.updateUserScore(user.getId(), score);
        user.setScore(score);
        return user;
    }
}
