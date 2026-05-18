package com.whut.training.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.LlmProviderRegistry;
import com.whut.training.service.llm.LlmGateway;
import com.whut.training.service.llm.LlmProperties;
import com.whut.training.service.impl.CodeforcesUserStatsSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class AdminAiProblemControllerIntegrationTest {

    private static final Path TEST_DB = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-admin-ai-problem-test-" + System.nanoTime() + ".db"
    ).toAbsolutePath();
    private static final Path TEST_STORAGE = Paths.get(
            System.getProperty("java.io.tmpdir"),
            "whut-training-admin-ai-problem-artifacts-" + System.nanoTime()
    ).toAbsolutePath();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DB.toString().replace("\\", "/"));
        registry.add("app.ai-problem.storage-root", () -> TEST_STORAGE.toString().replace("\\", "/"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenSessionRepository authTokenSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LlmProviderRegistry llmProviderRegistry;

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
    void adminCanCreateIterateSwitchPatchAndDownloadAiProblemDrafts() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN);
        String[] tokens = issueTokens(admin);
        stubLlmProvider();

        String createResponse = mockMvc.perform(post("/api/admin/ai-problems/sessions")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetRating": 1600,
                                  "targetTags": ["dp", "graphs"],
                                  "problemStyle": "constructive with tree flavor",
                                  "extraRequirements": "keep examples compact"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.session.status").value("READY"))
                .andExpect(jsonPath("$.data.draft.currentVersion").value(1))
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andExpect(jsonPath("$.data.artifactBundle.items[*].artifactType", hasItem("zip")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse).path("data");
        long sessionId = created.path("session").path("sessionId").asLong();
        long draftId = created.path("draft").path("draftId").asLong();

        mockMvc.perform(post("/api/admin/ai-problems/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Keep rating 1600, but make the main structure graph shortest path."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.draft.currentVersion").value(2))
                .andExpect(jsonPath("$.data.versions[0].versionNo").value(2))
                .andExpect(jsonPath("$.data.versions.length()").value(2));

        mockMvc.perform(post("/api/admin/ai-problems/drafts/" + draftId + "/versions/1/activate")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.draft.currentVersion").value(1))
                .andExpect(jsonPath("$.data.draft.title").value("Mirror Paths"));

        mockMvc.perform(patch("/api/admin/ai-problems/drafts/" + draftId)
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1])
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Mirror Paths Final",
                                  "rating": 1700,
                                  "tags": ["graphs", "shortest path"],
                                  "testPlanMd": "Add line graph and star graph coverage.",
                                  "samples": [
                                    { "input": "1", "output": "0", "explanation": "single node" }
                                  ],
                                  "tests": [
                                    { "name": "tiny", "input": "1", "output": "0" }
                                  ],
                                  "originalityNotice": "Manual originality review is still required."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.draft.title").value("Mirror Paths Final"))
                .andExpect(jsonPath("$.data.draft.rating").value(1700))
                .andExpect(jsonPath("$.data.draft.tags[0]").value("graphs"));

        mockMvc.perform(post("/api/admin/ai-problems/drafts/" + draftId + "/artifacts/regenerate")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items.length()", greaterThan(0)))
                .andExpect(jsonPath("$.data.items[*].artifactType", hasItem("zip")));

        mockMvc.perform(get("/api/admin/ai-problems/drafts/" + draftId + "/artifacts")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[*].artifactType", hasItem("statement")));

        mockMvc.perform(get("/api/admin/ai-problems/drafts/" + draftId + "/artifacts/download")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("problem-" + draftId + "-v1.zip")));

        mockMvc.perform(get("/api/admin/ai-problems/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.session.title").value("Mirror Paths Final"))
                .andExpect(jsonPath("$.data.versions.length()").value(2));
    }

    @Test
    void rejectsUnauthorizedAndNonAdminRequests() throws Exception {
        User normalUser = createUser("alice", UserRole.USER);
        String[] tokens = issueTokens(normalUser);

        mockMvc.perform(get("/api/admin/ai-problems/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/admin/ai-problems/sessions")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private void stubLlmProvider() {
        AtomicInteger callCount = new AtomicInteger();
        LlmGateway gateway = new LlmGateway() {
            @Override
            public String getType() {
                return "openai-compatible";
            }

            @Override
            public DraftGenerationResult generateProblemDraft(DraftGenerationRequest request, LlmProperties.ProviderProperties providerProperties) {
                int current = callCount.getAndIncrement();
                if (current == 0) {
                    return new DraftGenerationResult("Generated version 1.", "{}", buildProblemContent("Mirror Paths", 1600));
                }
                return new DraftGenerationResult("Generated version 2.", "{}", buildProblemContent("Mirror Paths Graph Remix", 1600));
            }
        };
        LlmProperties.ProviderProperties properties = new LlmProperties.ProviderProperties();
        properties.setBaseUrl("http://example.test/v1");
        properties.setApiKey("secret");
        properties.setModel("fake-model");

        LlmProviderRegistry.ResolvedProvider resolvedProvider =
                new LlmProviderRegistry.ResolvedProvider("test-provider", "fake-model", gateway, properties);
        when(llmProviderRegistry.resolve(isNull())).thenReturn(resolvedProvider);
        when(llmProviderRegistry.resolve(anyString())).thenReturn(resolvedProvider);
    }

    private AiProblemDtos.ProblemContent buildProblemContent(String title, int rating) {
        return new AiProblemDtos.ProblemContent(
                title,
                "Solve the mirror path problem.",
                "The first line contains n.",
                "Print the minimum answer.",
                "1 <= n <= 2e5",
                "Consider reversing the traversal order.",
                rating,
                List.of("dp", "graphs"),
                "Check corner cases with a single node.",
                List.of(
                        new AiProblemDtos.SampleItem("1", "0", "Only one node."),
                        new AiProblemDtos.SampleItem("3\n1 2\n2 3", "2", "A small chain.")
                ),
                "Cover line, star and balanced graph cases.",
                List.of(
                        new AiProblemDtos.TestCaseItem("tiny", "1", "0"),
                        new AiProblemDtos.TestCaseItem("chain", "3\n1 2\n2 3", "2")
                ),
                "Original draft, manual review required."
        );
    }

    private User createUser(String username, UserRole role) {
        User user = new User(
                null,
                username,
                username + "@example.com",
                "password123",
                role
        );
        return userRepository.save(user);
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
