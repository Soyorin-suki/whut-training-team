package com.whut.training.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.domain.entity.ai.AiProblemEntities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProblemArtifactServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void materializeArtifactsCreatesPreviewableFilesAndZip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiProblemArtifactService service = new AiProblemArtifactService(objectMapper, tempDir.toString());
        AiProblemDtos.ProblemContent problemContent = buildProblemContent();
        String serialized = objectMapper.writeValueAsString(problemContent);
        String now = Instant.now().toString();

        AiProblemEntities.Session session = new AiProblemEntities.Session(
                1L,
                problemContent.title(),
                99L,
                "test-provider",
                "fake-model",
                1600,
                "dp, trees",
                "READY",
                now,
                now
        );
        AiProblemEntities.Draft draft = new AiProblemEntities.Draft(
                2L,
                1L,
                1,
                problemContent.title(),
                problemContent.statementMd(),
                problemContent.inputSpecMd(),
                problemContent.outputSpecMd(),
                problemContent.constraintMd(),
                problemContent.hintMd(),
                problemContent.rating(),
                "dp, trees",
                "AI_ORIGINAL",
                problemContent.checkerNoteMd(),
                serialized,
                now,
                now
        );
        AiProblemEntities.Version version = new AiProblemEntities.Version(
                3L,
                2L,
                1,
                "prompt",
                "assistant",
                "{}",
                serialized,
                now
        );

        List<AiProblemEntities.Artifact> artifacts = service.materializeArtifacts(session, draft, version, problemContent);

        assertTrue(artifacts.stream().anyMatch(item -> "statement".equals(item.artifactType())));
        assertTrue(artifacts.stream().anyMatch(item -> "sample_input".equals(item.artifactType())));
        assertTrue(artifacts.stream().anyMatch(item -> "test_output".equals(item.artifactType())));
        assertTrue(artifacts.stream().anyMatch(item -> "zip".equals(item.artifactType())));

        AiProblemDtos.ArtifactBundleResponse bundle = service.buildArtifactBundle(draft, version, artifacts);
        assertNotNull(bundle);
        assertFalse(bundle.items().isEmpty());
        assertTrue(bundle.items().stream().anyMatch(item -> item.contentPreview() != null && item.contentPreview().contains("Mirror Paths")));
        assertTrue(service.loadZipBytes(1L, 2L, 1).length > 0);
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
