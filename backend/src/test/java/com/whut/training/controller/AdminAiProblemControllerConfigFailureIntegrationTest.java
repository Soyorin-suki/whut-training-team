package com.whut.training.controller;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.impl.CodeforcesUserStatsSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class AdminAiProblemControllerConfigFailureIntegrationTest {

    private static final String TEST_DB_NAME = "whut-training-admin-ai-problem-config-test-" + System.nanoTime();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:" + TEST_DB_NAME + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("app.llm.default-provider", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenSessionRepository authTokenSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private CodeforcesUserStatsSyncService codeforcesUserStatsSyncService;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM ai_problem_artifact");
        jdbcTemplate.update("DELETE FROM ai_problem_version");
        jdbcTemplate.update("DELETE FROM ai_problem_draft");
        jdbcTemplate.update("DELETE FROM ai_problem_message");
        jdbcTemplate.update("DELETE FROM ai_problem_session");
        jdbcTemplate.update("DELETE FROM auth_token_session");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void returnsClearConfigErrorWhenNoProviderIsConfigured() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN);
        String[] tokens = issueTokens(admin);

        mockMvc.perform(post("/api/admin/ai-problems/sessions")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1])
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetRating": 1600,
                                  "targetTags": ["dp", "graphs"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("llm provider is not configured"));
    }

    private User createUser(String username, UserRole role) {
        return userRepository.save(new User(
                null,
                username,
                username + "@example.com",
                "password123",
                role
        ));
    }

    private String[] issueTokens(User user) {
        String accessToken = "access-" + user.getUsername();
        String refreshToken = "refresh-" + user.getUsername();
        authTokenSessionRepository.save(new AuthTokenSession(
                user.getId(),
                accessToken,
                refreshToken,
                Instant.now().plusSeconds(3600).getEpochSecond(),
                Instant.now().plusSeconds(7200).getEpochSecond()
        ));
        return new String[]{accessToken, refreshToken};
    }
}
