package com.whut.training.controller;

import com.whut.training.domain.entity.CfProblem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.entity.UserPracticeDraw;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.AuthTokenSessionRepository;
import com.whut.training.repository.AuthTokenSessionRepository.AuthTokenSession;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
class AdminTrainingControllerIntegrationTest {

    private static final String TEST_DB_NAME = "whut-training-admin-dashboard-test-" + System.nanoTime();

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
    private DailyProblemRepository dailyProblemRepository;

    @Autowired
    private AuthTokenSessionRepository authTokenSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final DataFormatter dataFormatter = new DataFormatter();

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM auth_token_session");
        jdbcTemplate.update("DELETE FROM user_daily_status");
        jdbcTemplate.update("DELETE FROM user_practice_draw");
        jdbcTemplate.update("DELETE FROM daily_problem");
        jdbcTemplate.update("DELETE FROM cf_problem");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void returnsOverviewDailyRecordsAndUserTimelineForAdmin() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN, 0);
        User alice = createUser("alice", UserRole.USER, 1600);
        User bob = createUser("bob", UserRole.USER, 1200);

        String[] adminTokens = issueTokens(admin);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        CfProblem todayDailyProblem = createProblem("2000-A", 2000, "A", "Today Daily", 1500);
        CfProblem yesterdayDailyProblem = createProblem("1999-B", 1999, "B", "Yesterday Daily", 1400);
        CfProblem practiceProblem = createProblem("1888-C", 1888, "C", "Practice C", 1700);
        CfProblem practiceProblemTwo = createProblem("1777-D", 1777, "D", "Practice D", 1500);

        dailyProblemRepository.insertDailyProblem(today, todayDailyProblem, "admin");
        dailyProblemRepository.insertDailyProblem(yesterday, yesterdayDailyProblem, "scheduler");

        dailyProblemRepository.saveUserDailyStatus(alice.getId(), today, 10001L, "OK", 1);
        dailyProblemRepository.saveUserDailyStatus(alice.getId(), yesterday, 10002L, "OK", 1);
        userRepository.updateUserStreakStats(alice.getId(), 2, 4);
        userRepository.updateUserStreakStats(bob.getId(), 1, 3);

        UserPracticeDraw alicePractice = dailyProblemRepository.insertPracticeDraw(alice.getId(), today, practiceProblem);
        dailyProblemRepository.updatePracticeCheck(alicePractice.id(), alice.getId(), 20001L, "OK");

        UserPracticeDraw bobPractice = dailyProblemRepository.insertPracticeDraw(bob.getId(), today, practiceProblemTwo);
        dailyProblemRepository.updatePracticeCheck(bobPractice.id(), bob.getId(), 20002L, "WRONG_ANSWER");

        mockMvc.perform(get("/api/admin/training/overview")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.date").value(today.toString()))
                .andExpect(jsonPath("$.data.problem.name").value("Today Daily"))
                .andExpect(jsonPath("$.data.totalUsers").value(3))
                .andExpect(jsonPath("$.data.activeUsers").value(2))
                .andExpect(jsonPath("$.data.dailyCheckInCount").value(1))
                .andExpect(jsonPath("$.data.pendingDailyUserCount").value(2))
                .andExpect(jsonPath("$.data.practiceDrawCount").value(2))
                .andExpect(jsonPath("$.data.practiceCheckCount").value(2))
                .andExpect(jsonPath("$.data.todayStreakUserCount").value(1))
                .andExpect(jsonPath("$.data.maxCurrentStreakDays").value(2))
                .andExpect(jsonPath("$.data.maxLongestStreakDays").value(4))
                .andExpect(jsonPath("$.data.averageCurrentStreakDays").value(1.5d));

        mockMvc.perform(get("/api/admin/training/daily-records")
                        .param("startDate", yesterday.toString())
                        .param("endDate", today.toString())
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.entries[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data.entries[0].problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.entries[0].dailyCheckInCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].practiceCheckCount").value(2))
                .andExpect(jsonPath("$.data.entries[1].date").value(yesterday.toString()))
                .andExpect(jsonPath("$.data.entries[1].dailyCheckInCount").value(1));

        mockMvc.perform(get("/api/admin/training/daily-records/" + today)
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem.problemKey").value("2000-A"))
                .andExpect(jsonPath("$.data.checkIns.length()").value(1))
                .andExpect(jsonPath("$.data.checkIns[0].username").value("alice"))
                .andExpect(jsonPath("$.data.practiceChecks.length()").value(2));

        mockMvc.perform(get("/api/admin/training/users")
                        .param("keyword", "ali")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.entries[0].username").value("alice"))
                .andExpect(jsonPath("$.data.entries[0].dailyCheckInCount").value(2))
                .andExpect(jsonPath("$.data.entries[0].practiceCheckCount").value(1))
                .andExpect(jsonPath("$.data.entries[0].currentStreakDays").value(2))
                .andExpect(jsonPath("$.data.entries[0].longestStreakDays").value(4));

        mockMvc.perform(get("/api/admin/training/users/" + alice.getId() + "/timeline")
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(alice.getId()))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.currentStreakDays").value(2))
                .andExpect(jsonPath("$.data.longestStreakDays").value(4))
                .andExpect(jsonPath("$.data.entries.length()").value(3))
                .andExpect(jsonPath("$.data.entries[0].activityType").value("PRACTICE"))
                .andExpect(jsonPath("$.data.entries[1].activityType").value("DAILY"));
    }

    @Test
    void rejectsNonAdminUsers() throws Exception {
        User user = createUser("alice", UserRole.USER, 1000);
        String[] tokens = issueTokens(user);

        mockMvc.perform(get("/api/admin/training/overview")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("admin role required"));

        mockMvc.perform(get("/api/admin/training/export")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("admin role required"));
    }

    @Test
    void rejectsUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/admin/training/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/admin/training/export"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void returnsEmptyDailyRecordPagesWhenNoTrainingDataExists() throws Exception {
        User admin = createUser("admin", UserRole.ADMIN, 0);
        String[] tokens = issueTokens(admin);
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/admin/training/overview")
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.problem").value(nullValue()))
                .andExpect(jsonPath("$.data.dailyCheckInCount").value(0))
                .andExpect(jsonPath("$.data.practiceCheckCount").value(0))
                .andExpect(jsonPath("$.data.todayStreakUserCount").value(0))
                .andExpect(jsonPath("$.data.maxCurrentStreakDays").value(0))
                .andExpect(jsonPath("$.data.maxLongestStreakDays").value(0))
                .andExpect(jsonPath("$.data.averageCurrentStreakDays").value(0.0d));

        mockMvc.perform(get("/api/admin/training/daily-records")
                        .param("startDate", today.minusDays(1).toString())
                        .param("endDate", today.toString())
                        .header("Authorization", "Bearer " + tokens[0])
                        .header("X-Refresh-Token", tokens[1]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.entries.length()").value(0));
    }

    @Test
    void exportsTrainingWorkbookForAdmin() throws Exception {
        User admin = createDetailedUser("admin", UserRole.ADMIN, null, null, 0, 0, 0, 0, 0, 0, 0, 0);
        User alice = createDetailedUser("alice", UserRole.USER, 1543, 1688, 320, 45, 9, 7, 5, 1, 4, 6);
        User bob = createDetailedUser("bob", UserRole.USER, 1820, 1902, 410, 61, 14, 9, 6, 3, 2, 7);
        String[] adminTokens = issueTokens(admin);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        CfProblem todayDailyProblem = createProblem("2000-A", 2000, "A", "Today Daily", 1500);
        CfProblem yesterdayDailyProblem = createProblem("1999-B", 1999, "B", "Yesterday Daily", 1400);
        CfProblem practiceProblem = createProblem("1888-C", 1888, "C", "Practice C", 1700);
        CfProblem ignoredPracticeProblem = createProblem("1777-D", 1777, "D", "Ignored Practice", 1500);

        dailyProblemRepository.insertDailyProblem(today, todayDailyProblem, "admin");
        dailyProblemRepository.insertDailyProblem(yesterday, yesterdayDailyProblem, "scheduler");

        dailyProblemRepository.saveUserDailyStatus(alice.getId(), today, 11001L, "OK", 1);
        dailyProblemRepository.saveUserDailyStatus(alice.getId(), yesterday, 11002L, "WRONG_ANSWER", 0);
        jdbcTemplate.update(
                "UPDATE user_daily_status SET checked_at = ? WHERE user_id = ? AND date = ?",
                today + "T09:30:00+08:00",
                alice.getId(),
                today.toString()
        );
        jdbcTemplate.update(
                "UPDATE user_daily_status SET checked_at = ? WHERE user_id = ? AND date = ?",
                yesterday + "T07:45:00+08:00",
                alice.getId(),
                yesterday.toString()
        );

        UserPracticeDraw exportedPractice = dailyProblemRepository.insertPracticeDraw(bob.getId(), today, practiceProblem);
        dailyProblemRepository.updatePracticeCheck(exportedPractice.id(), bob.getId(), 22001L, "OK");
        jdbcTemplate.update(
                "UPDATE user_practice_draw SET checked_at = ? WHERE id = ?",
                today + "T10:00:00+08:00",
                exportedPractice.id()
        );
        dailyProblemRepository.insertPracticeDraw(alice.getId(), today, ignoredPracticeProblem);

        MvcResult result = mockMvc.perform(get("/api/admin/training/export")
                        .header("Authorization", "Bearer " + adminTokens[0])
                        .header("X-Refresh-Token", adminTokens[1]))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"admin-training-export-" + today + ".xlsx\""
                ))
                .andReturn();

        byte[] workbookBytes = result.getResponse().getContentAsByteArray();
        assertTrue(workbookBytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("Training Export", sheet.getSheetName());
            assertEquals("用户名", cellText(sheet, 0, 0));
            assertEquals("当前Rating", cellText(sheet, 0, 1));
            assertEquals("打卡天数", cellText(sheet, 0, 2));
            assertEquals("800-1400 Rating解决题目数", cellText(sheet, 0, 3));
            assertEquals("1500-2200 Rating解决题目数", cellText(sheet, 0, 4));
            assertEquals("2200以上 Rating解决题目数", cellText(sheet, 0, 5));

            assertEquals(3, sheet.getLastRowNum());

            assertEquals("admin", cellText(sheet, 1, 0));
            assertEquals("", cellText(sheet, 1, 1));
            assertEquals("0", cellText(sheet, 1, 2));
            assertEquals("0", cellText(sheet, 1, 3));
            assertEquals("0", cellText(sheet, 1, 4));
            assertEquals("0", cellText(sheet, 1, 5));

            assertEquals("alice", cellText(sheet, 2, 0));
            assertEquals("1543", cellText(sheet, 2, 1));
            assertEquals("2", cellText(sheet, 2, 2));
            assertEquals("7", cellText(sheet, 2, 3));
            assertEquals("5", cellText(sheet, 2, 4));
            assertEquals("1", cellText(sheet, 2, 5));

            assertEquals("bob", cellText(sheet, 3, 0));
            assertEquals("1820", cellText(sheet, 3, 1));
            assertEquals("0", cellText(sheet, 3, 2));
            assertEquals("9", cellText(sheet, 3, 3));
            assertEquals("6", cellText(sheet, 3, 4));
            assertEquals("3", cellText(sheet, 3, 5));
        }
    }

    private User createUser(String username, UserRole role, int score) {
        return createDetailedUser(username, role, null, null, score, 0, 0, 0, 0, 0, 0, 0);
    }

    private User createDetailedUser(
            String username,
            UserRole role,
            Integer codeforcesRating,
            Integer maxRating,
            Integer score,
            Integer solvedProblemCount,
            Integer hardSolvedProblemCount,
            Integer solved800To1400Count,
            Integer solved1500To2200Count,
            Integer solvedAbove2200Count,
            Integer currentStreakDays,
            Integer longestStreakDays
    ) {
        User user = new User(
                null,
                username,
                username + "@example.com",
                "password123",
                role
        );
        user.setCodeforcesRating(codeforcesRating);
        user.setMaxRating(maxRating);
        user.setScore(score);
        user.setSolvedProblemCount(solvedProblemCount);
        user.setHardSolvedProblemCount(hardSolvedProblemCount);
        user.setSolved800To1400Count(solved800To1400Count);
        user.setSolved1500To2200Count(solved1500To2200Count);
        user.setSolvedAbove2200Count(solvedAbove2200Count);
        user.setCurrentStreakDays(currentStreakDays);
        user.setLongestStreakDays(longestStreakDays);
        userRepository.save(user);
        return user;
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

    private CfProblem createProblem(String problemKey, int contestId, String problemIndex, String name, int rating) {
        return new CfProblem(
                problemKey,
                contestId,
                problemIndex,
                name,
                rating,
                "dp,graphs",
                false,
                null,
                100,
                "https://codeforces.com/problemset/problem/" + contestId + "/" + problemIndex
        );
    }

    private String cellText(Sheet sheet, int rowIndex, int cellIndex) {
        if (sheet.getRow(rowIndex) == null || sheet.getRow(rowIndex).getCell(cellIndex) == null) {
            return "";
        }
        return dataFormatter.formatCellValue(sheet.getRow(rowIndex).getCell(cellIndex));
    }
}
