package com.whut.training.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.domain.entity.ai.AiProblemEntities;
import com.whut.training.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AiProblemArtifactService {

    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public AiProblemArtifactService(
            ObjectMapper objectMapper,
            @Value("${app.ai-problem.storage-root:./data/ai-problems}") String storageRoot
    ) {
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    public List<AiProblemEntities.Artifact> materializeArtifacts(
            AiProblemEntities.Session session,
            AiProblemEntities.Draft draft,
            AiProblemEntities.Version version,
            AiProblemDtos.ProblemContent problemContent
    ) {
        Path versionRoot = resolveVersionRoot(session.id(), version.versionNo());
        Path packageDir = resolvePackageDir(versionRoot, draft.id(), version.versionNo());
        try {
            Files.createDirectories(storageRoot);
            deleteRecursively(versionRoot);
            Files.createDirectories(packageDir);

            List<AiProblemEntities.Artifact> artifacts = new ArrayList<>();
            String createdAt = Instant.now().toString();
            String packageFolder = packageDir.getFileName().toString();

            Path metaPath = writeTextFile(
                    packageDir.resolve("meta.json"),
                    toPrettyJson(buildMeta(session, draft, version, problemContent))
            );
            artifacts.add(toArtifact(version.id(), "meta", packageFolder, metaPath, createdAt, "application/json"));

            Path statementPath = writeTextFile(packageDir.resolve("statement.md"), buildStatementMarkdown(problemContent));
            artifacts.add(toArtifact(version.id(), "statement", packageFolder, statementPath, createdAt, "text/markdown"));

            if (problemContent.checkerNoteMd() != null && !problemContent.checkerNoteMd().isBlank()) {
                Path checkerNotePath = writeTextFile(packageDir.resolve("checker-note.md"), problemContent.checkerNoteMd());
                artifacts.add(toArtifact(version.id(), "checker_note", packageFolder, checkerNotePath, createdAt, "text/markdown"));
            }

            Path samplesDir = Files.createDirectories(packageDir.resolve("samples"));
            for (int i = 0; i < problemContent.samples().size(); i++) {
                AiProblemDtos.SampleItem sample = problemContent.samples().get(i);
                int sampleNo = i + 1;
                Path inputPath = writeTextFile(samplesDir.resolve("sample" + sampleNo + ".in"), sample.input());
                artifacts.add(toArtifact(version.id(), "sample_input", packageFolder, inputPath, createdAt, "text/plain"));
                Path outputPath = writeTextFile(samplesDir.resolve("sample" + sampleNo + ".out"), sample.output());
                artifacts.add(toArtifact(version.id(), "sample_output", packageFolder, outputPath, createdAt, "text/plain"));
            }

            Path testsDir = Files.createDirectories(packageDir.resolve("tests"));
            for (int i = 0; i < problemContent.tests().size(); i++) {
                AiProblemDtos.TestCaseItem testCase = problemContent.tests().get(i);
                String prefix = String.format("%02d", i + 1);
                Path inputPath = writeTextFile(testsDir.resolve(prefix + ".in"), testCase.input());
                artifacts.add(toArtifact(version.id(), "test_input", packageFolder, inputPath, createdAt, "text/plain"));
                Path outputPath = writeTextFile(testsDir.resolve(prefix + ".out"), testCase.output());
                artifacts.add(toArtifact(version.id(), "test_output", packageFolder, outputPath, createdAt, "text/plain"));
            }

            Path zipPath = versionRoot.resolve(packageFolder + ".zip");
            writeZip(packageDir, zipPath);
            artifacts.add(toArtifact(version.id(), "zip", null, zipPath, createdAt, "application/zip"));

            return artifacts;
        } catch (IOException ex) {
            throw new BusinessException(500, "failed to materialize ai problem artifacts");
        }
    }

    public AiProblemDtos.ArtifactBundleResponse buildArtifactBundle(
            AiProblemEntities.Draft draft,
            AiProblemEntities.Version version,
            List<AiProblemEntities.Artifact> artifacts
    ) {
        List<AiProblemDtos.ArtifactView> items = artifacts.stream()
                .map(artifact -> new AiProblemDtos.ArtifactView(
                        artifact.id(),
                        artifact.artifactType(),
                        artifact.fileName(),
                        artifact.relativePath(),
                        artifact.contentType(),
                        artifact.sizeBytes(),
                        artifact.createdAt(),
                        loadPreviewContent(draft.sessionId(), version.versionNo(), artifact)
                ))
                .toList();
        return new AiProblemDtos.ArtifactBundleResponse(
                draft.id(),
                version.versionNo(),
                buildDownloadUrl(draft.id()),
                items
        );
    }

    public byte[] loadZipBytes(Long sessionId, Long draftId, Integer versionNo) {
        Path zipPath = resolveVersionRoot(sessionId, versionNo)
                .resolve("problem-" + draftId + "-v" + versionNo + ".zip");
        try {
            if (!Files.exists(zipPath)) {
                throw new BusinessException(404, "artifact zip not found");
            }
            return Files.readAllBytes(zipPath);
        } catch (IOException ex) {
            throw new BusinessException(500, "failed to load artifact zip");
        }
    }

    public String buildDownloadUrl(Long draftId) {
        return "/api/admin/ai-problems/drafts/" + draftId + "/artifacts/download";
    }

    private Map<String, Object> buildMeta(
            AiProblemEntities.Session session,
            AiProblemEntities.Draft draft,
            AiProblemEntities.Version version,
            AiProblemDtos.ProblemContent problemContent
    ) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("draftId", draft.id());
        meta.put("sessionId", session.id());
        meta.put("version", version.versionNo());
        meta.put("title", problemContent.title());
        meta.put("rating", problemContent.rating());
        meta.put("tags", problemContent.tags());
        meta.put("generatedAt", version.createdAt());
        meta.put("providerKey", session.providerKey());
        meta.put("modelName", session.modelName());
        meta.put("originalityNotice", problemContent.originalityNotice());
        return meta;
    }

    private String buildStatementMarkdown(AiProblemDtos.ProblemContent problemContent) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(problemContent.title()).append("\n\n");
        builder.append(problemContent.statementMd()).append("\n\n");
        builder.append("## Input\n\n").append(problemContent.inputSpecMd()).append("\n\n");
        builder.append("## Output\n\n").append(problemContent.outputSpecMd()).append("\n\n");
        builder.append("## Constraints\n\n").append(problemContent.constraintMd()).append("\n\n");
        if (problemContent.hintMd() != null && !problemContent.hintMd().isBlank()) {
            builder.append("## Hint\n\n").append(problemContent.hintMd()).append("\n\n");
        }
        builder.append("## Originality Notice\n\n").append(problemContent.originalityNotice()).append("\n");
        return builder.toString();
    }

    private AiProblemEntities.Artifact toArtifact(
            Long versionId,
            String artifactType,
            String packageFolder,
            Path filePath,
            String createdAt,
            String contentType
    ) throws IOException {
        String relativePath;
        if (packageFolder == null) {
            relativePath = filePath.getFileName().toString();
        } else {
            relativePath = packageFolder + "/" + packageDirRelativePath(filePath, packageFolder);
        }
        return new AiProblemEntities.Artifact(
                null,
                versionId,
                artifactType,
                filePath.getFileName().toString(),
                relativePath.replace("\\", "/"),
                contentType,
                Files.size(filePath),
                createdAt
        );
    }

    private String packageDirRelativePath(Path filePath, String packageFolder) {
        Path packagePath = filePath;
        while (packagePath != null && !packageFolder.equals(packagePath.getFileName() == null ? null : packagePath.getFileName().toString())) {
            packagePath = packagePath.getParent();
        }
        if (packagePath == null) {
            return filePath.getFileName().toString();
        }
        return packagePath.relativize(filePath).toString();
    }

    private Path writeTextFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        return Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    private String toPrettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "failed to serialize artifact metadata");
        }
    }

    private void writeZip(Path sourceDir, Path zipPath) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            try (Stream<Path> stream = Files.walk(sourceDir)) {
                stream.filter(Files::isRegularFile)
                        .sorted()
                        .forEach(path -> writeZipEntry(sourceDir, path, zipOutputStream));
            }
            zipOutputStream.finish();
            Files.write(zipPath, outputStream.toByteArray());
        }
    }

    private void writeZipEntry(Path sourceDir, Path file, ZipOutputStream zipOutputStream) {
        String entryName = sourceDir.getFileName() + "/" + sourceDir.relativize(file).toString().replace("\\", "/");
        try {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(Files.readAllBytes(file));
            zipOutputStream.closeEntry();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String loadPreviewContent(Long sessionId, Integer versionNo, AiProblemEntities.Artifact artifact) {
        if ("zip".equalsIgnoreCase(artifact.artifactType())) {
            return null;
        }
        Path path = resolveVersionRoot(sessionId, versionNo).resolve(artifact.relativePath()).normalize();
        if (!path.startsWith(storageRoot) || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return null;
        }
    }

    private Path resolveVersionRoot(Long sessionId, Integer versionNo) {
        return storageRoot.resolve(String.valueOf(sessionId)).resolve("v" + versionNo).normalize();
    }

    private Path resolvePackageDir(Path versionRoot, Long draftId, Integer versionNo) {
        return versionRoot.resolve("problem-" + draftId + "-v" + versionNo).normalize();
    }

    private void deleteRecursively(Path target) throws IOException {
        if (target == null || !Files.exists(target)) {
            return;
        }
        if (!target.normalize().startsWith(storageRoot)) {
            throw new BusinessException(500, "refusing to delete artifact path outside storage root");
        }
        try (Stream<Path> stream = Files.walk(target)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        } catch (UncheckedIOException ex) {
            throw new IOException(ex.getCause());
        }
    }
}
