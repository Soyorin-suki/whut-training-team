package com.whut.training.service;

import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProblemValidatorTest {

    private final AiProblemValidator validator = new AiProblemValidator();

    @Test
    void normalizeProblemContentAcceptsCompletePayload() {
        AiProblemDtos.ProblemContent normalized = validator.normalizeProblemContent(buildProblemContent());

        assertEquals("Mirror Paths", normalized.title());
        assertEquals(List.of("dp", "trees"), normalized.tags());
        assertEquals(2, normalized.tests().size());
    }

    @Test
    void normalizeProblemContentRejectsInvalidRating() {
        AiProblemDtos.ProblemContent invalid = new AiProblemDtos.ProblemContent(
                "Mirror Paths",
                "statement",
                "input",
                "output",
                "constraints",
                "hint",
                700,
                List.of("dp"),
                null,
                List.of(new AiProblemDtos.SampleItem("1", "1", null)),
                "plan",
                List.of(new AiProblemDtos.TestCaseItem("tiny", "1", "1")),
                "Original draft, manual review required."
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.normalizeProblemContent(invalid));

        assertEquals(400, ex.getCode());
        assertEquals("rating must be between 800 and 3500", ex.getMessage());
    }

    @Test
    void normalizeProblemContentRejectsEmptySamples() {
        AiProblemDtos.ProblemContent invalid = new AiProblemDtos.ProblemContent(
                "Mirror Paths",
                "statement",
                "input",
                "output",
                "constraints",
                "hint",
                1600,
                List.of("dp"),
                null,
                List.of(),
                "plan",
                List.of(new AiProblemDtos.TestCaseItem("tiny", "1", "1")),
                "Original draft, manual review required."
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.normalizeProblemContent(invalid));

        assertEquals(400, ex.getCode());
        assertEquals("samples must not be empty", ex.getMessage());
    }

    @Test
    void mergePatchReplacesEditableSections() {
        AiProblemDtos.ProblemContent merged = validator.mergePatch(
                buildProblemContent(),
                new AiProblemDtos.DraftPatchRequest(
                        "Mirror Paths Revised",
                        "updated statement",
                        null,
                        null,
                        null,
                        null,
                        1700,
                        List.of("graphs", "shortest path"),
                        null,
                        List.of(new AiProblemDtos.SampleItem("2", "3", "patched")),
                        "updated plan",
                        List.of(new AiProblemDtos.TestCaseItem("patched", "2", "3")),
                        "Still requires manual originality review."
                )
        );

        assertEquals("Mirror Paths Revised", merged.title());
        assertEquals(1700, merged.rating());
        assertEquals(List.of("graphs", "shortest path"), merged.tags());
        assertEquals("2", merged.samples().get(0).input());
    }

    private AiProblemDtos.ProblemContent buildProblemContent() {
        return new AiProblemDtos.ProblemContent(
                "Mirror Paths",
                "Solve the mirror path problem.",
                "The first line contains n.",
                "Print the minimum answer.",
                "1 <= n <= 2e5",
                "Consider reversing the traversal order.",
                1600,
                List.of("dp", "trees"),
                "Check corner cases with a single node.",
                List.of(
                        new AiProblemDtos.SampleItem("1", "0", "Only one node."),
                        new AiProblemDtos.SampleItem("3\n1 2\n2 3", "2", "A small chain.")
                ),
                "Cover line, star and balanced tree cases.",
                List.of(
                        new AiProblemDtos.TestCaseItem("tiny", "1", "0"),
                        new AiProblemDtos.TestCaseItem("chain", "3\n1 2\n2 3", "2")
                ),
                "Original draft, manual review required."
        );
    }
}
