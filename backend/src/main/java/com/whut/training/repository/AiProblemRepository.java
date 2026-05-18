package com.whut.training.repository;

import com.whut.training.domain.entity.ai.AiProblemEntities;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AiProblemRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AiProblemEntities.Session> sessionRowMapper = (rs, rowNum) -> new AiProblemEntities.Session(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getLong("created_by"),
            rs.getString("provider_key"),
            rs.getString("model_name"),
            (Integer) rs.getObject("target_rating"),
            rs.getString("target_tags"),
            rs.getString("status"),
            rs.getString("created_at"),
            rs.getString("updated_at")
    );
    private final RowMapper<AiProblemEntities.Message> messageRowMapper = (rs, rowNum) -> new AiProblemEntities.Message(
            rs.getLong("id"),
            rs.getLong("session_id"),
            rs.getString("role"),
            rs.getString("content"),
            rs.getString("prompt_context_json"),
            rs.getString("created_at")
    );
    private final RowMapper<AiProblemEntities.Draft> draftRowMapper = (rs, rowNum) -> new AiProblemEntities.Draft(
            rs.getLong("id"),
            rs.getLong("session_id"),
            rs.getInt("current_version"),
            rs.getString("title"),
            rs.getString("statement_md"),
            rs.getString("input_spec_md"),
            rs.getString("output_spec_md"),
            rs.getString("constraint_md"),
            rs.getString("hint_md"),
            (Integer) rs.getObject("rating"),
            rs.getString("tags"),
            rs.getString("source_type"),
            rs.getString("checker_note_md"),
            rs.getString("normalized_problem_json"),
            rs.getString("created_at"),
            rs.getString("updated_at")
    );
    private final RowMapper<AiProblemEntities.Version> versionRowMapper = (rs, rowNum) -> new AiProblemEntities.Version(
            rs.getLong("id"),
            rs.getLong("draft_id"),
            rs.getInt("version_no"),
            rs.getString("generation_prompt"),
            rs.getString("assistant_message"),
            rs.getString("llm_response_json"),
            rs.getString("normalized_problem_json"),
            rs.getString("created_at")
    );
    private final RowMapper<AiProblemEntities.Artifact> artifactRowMapper = (rs, rowNum) -> new AiProblemEntities.Artifact(
            rs.getLong("id"),
            rs.getLong("version_id"),
            rs.getString("artifact_type"),
            rs.getString("file_name"),
            rs.getString("relative_path"),
            rs.getString("content_type"),
            rs.getLong("size_bytes"),
            rs.getString("created_at")
    );

    public AiProblemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiProblemEntities.Session saveSession(AiProblemEntities.Session session) {
        Long id = insertAndReturnId(
                """
                        INSERT INTO ai_problem_session (
                            title, created_by, provider_key, model_name, target_rating, target_tags, status, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                session.title(),
                session.createdBy(),
                session.providerKey(),
                session.modelName(),
                session.targetRating(),
                session.targetTags(),
                session.status(),
                session.createdAt(),
                session.updatedAt()
        );
        return new AiProblemEntities.Session(
                id,
                session.title(),
                session.createdBy(),
                session.providerKey(),
                session.modelName(),
                session.targetRating(),
                session.targetTags(),
                session.status(),
                session.createdAt(),
                session.updatedAt()
        );
    }

    public void updateSession(AiProblemEntities.Session session) {
        jdbcTemplate.update(
                """
                        UPDATE ai_problem_session
                        SET title = ?, provider_key = ?, model_name = ?, target_rating = ?, target_tags = ?, status = ?, updated_at = ?
                        WHERE id = ?
                        """,
                session.title(),
                session.providerKey(),
                session.modelName(),
                session.targetRating(),
                session.targetTags(),
                session.status(),
                session.updatedAt(),
                session.id()
        );
    }

    public Optional<AiProblemEntities.Session> findSessionById(Long sessionId) {
        List<AiProblemEntities.Session> rows = jdbcTemplate.query(
                """
                        SELECT id, title, created_by, provider_key, model_name, target_rating, target_tags, status, created_at, updated_at
                        FROM ai_problem_session
                        WHERE id = ?
                        """,
                sessionRowMapper,
                sessionId
        );
        return rows.stream().findFirst();
    }

    public long countSessions(String keyword, String status) {
        QueryParts queryParts = buildSessionFilter(keyword, status, true);
        Long total = jdbcTemplate.queryForObject(queryParts.sql(), Long.class, queryParts.params());
        return total == null ? 0L : total;
    }

    public List<AiProblemEntities.Session> findSessions(String keyword, String status, int limit, int offset) {
        QueryParts queryParts = buildSessionFilter(keyword, status, false);
        List<Object> params = new ArrayList<>(queryParts.params().length + 2);
        for (Object param : queryParts.params()) {
            params.add(param);
        }
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(
                queryParts.sql() + " ORDER BY updated_at DESC, id DESC LIMIT ? OFFSET ?",
                sessionRowMapper,
                params.toArray()
        );
    }

    public AiProblemEntities.Message saveMessage(AiProblemEntities.Message message) {
        Long id = insertAndReturnId(
                """
                        INSERT INTO ai_problem_message (session_id, role, content, prompt_context_json, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                message.sessionId(),
                message.role(),
                message.content(),
                message.promptContextJson(),
                message.createdAt()
        );
        return new AiProblemEntities.Message(
                id,
                message.sessionId(),
                message.role(),
                message.content(),
                message.promptContextJson(),
                message.createdAt()
        );
    }

    public List<AiProblemEntities.Message> findMessagesBySessionId(Long sessionId) {
        return jdbcTemplate.query(
                """
                        SELECT id, session_id, role, content, prompt_context_json, created_at
                        FROM ai_problem_message
                        WHERE session_id = ?
                        ORDER BY id ASC
                        """,
                messageRowMapper,
                sessionId
        );
    }

    public AiProblemEntities.Draft saveDraft(AiProblemEntities.Draft draft) {
        Long id = insertAndReturnId(
                """
                        INSERT INTO ai_problem_draft (
                            session_id, current_version, title, statement_md, input_spec_md, output_spec_md, constraint_md,
                            hint_md, rating, tags, source_type, checker_note_md, normalized_problem_json, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                draft.sessionId(),
                draft.currentVersion(),
                draft.title(),
                draft.statementMd(),
                draft.inputSpecMd(),
                draft.outputSpecMd(),
                draft.constraintMd(),
                draft.hintMd(),
                draft.rating(),
                draft.tags(),
                draft.sourceType(),
                draft.checkerNoteMd(),
                draft.normalizedProblemJson(),
                draft.createdAt(),
                draft.updatedAt()
        );
        return new AiProblemEntities.Draft(
                id,
                draft.sessionId(),
                draft.currentVersion(),
                draft.title(),
                draft.statementMd(),
                draft.inputSpecMd(),
                draft.outputSpecMd(),
                draft.constraintMd(),
                draft.hintMd(),
                draft.rating(),
                draft.tags(),
                draft.sourceType(),
                draft.checkerNoteMd(),
                draft.normalizedProblemJson(),
                draft.createdAt(),
                draft.updatedAt()
        );
    }

    public void updateDraft(AiProblemEntities.Draft draft) {
        jdbcTemplate.update(
                """
                        UPDATE ai_problem_draft
                        SET current_version = ?, title = ?, statement_md = ?, input_spec_md = ?, output_spec_md = ?, constraint_md = ?,
                            hint_md = ?, rating = ?, tags = ?, source_type = ?, checker_note_md = ?, normalized_problem_json = ?, updated_at = ?
                        WHERE id = ?
                        """,
                draft.currentVersion(),
                draft.title(),
                draft.statementMd(),
                draft.inputSpecMd(),
                draft.outputSpecMd(),
                draft.constraintMd(),
                draft.hintMd(),
                draft.rating(),
                draft.tags(),
                draft.sourceType(),
                draft.checkerNoteMd(),
                draft.normalizedProblemJson(),
                draft.updatedAt(),
                draft.id()
        );
    }

    public Optional<AiProblemEntities.Draft> findDraftBySessionId(Long sessionId) {
        List<AiProblemEntities.Draft> rows = jdbcTemplate.query(
                """
                        SELECT id, session_id, current_version, title, statement_md, input_spec_md, output_spec_md, constraint_md,
                               hint_md, rating, tags, source_type, checker_note_md, normalized_problem_json, created_at, updated_at
                        FROM ai_problem_draft
                        WHERE session_id = ?
                        LIMIT 1
                        """,
                draftRowMapper,
                sessionId
        );
        return rows.stream().findFirst();
    }

    public Optional<AiProblemEntities.Draft> findDraftById(Long draftId) {
        List<AiProblemEntities.Draft> rows = jdbcTemplate.query(
                """
                        SELECT id, session_id, current_version, title, statement_md, input_spec_md, output_spec_md, constraint_md,
                               hint_md, rating, tags, source_type, checker_note_md, normalized_problem_json, created_at, updated_at
                        FROM ai_problem_draft
                        WHERE id = ?
                        LIMIT 1
                        """,
                draftRowMapper,
                draftId
        );
        return rows.stream().findFirst();
    }

    public AiProblemEntities.Version saveVersion(AiProblemEntities.Version version) {
        Long id = insertAndReturnId(
                """
                        INSERT INTO ai_problem_version (
                            draft_id, version_no, generation_prompt, assistant_message, llm_response_json, normalized_problem_json, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                version.draftId(),
                version.versionNo(),
                version.generationPrompt(),
                version.assistantMessage(),
                version.llmResponseJson(),
                version.normalizedProblemJson(),
                version.createdAt()
        );
        return new AiProblemEntities.Version(
                id,
                version.draftId(),
                version.versionNo(),
                version.generationPrompt(),
                version.assistantMessage(),
                version.llmResponseJson(),
                version.normalizedProblemJson(),
                version.createdAt()
        );
    }

    public Optional<AiProblemEntities.Version> findVersionByDraftIdAndVersionNo(Long draftId, int versionNo) {
        List<AiProblemEntities.Version> rows = jdbcTemplate.query(
                """
                        SELECT id, draft_id, version_no, generation_prompt, assistant_message, llm_response_json, normalized_problem_json, created_at
                        FROM ai_problem_version
                        WHERE draft_id = ? AND version_no = ?
                        LIMIT 1
                        """,
                versionRowMapper,
                draftId,
                versionNo
        );
        return rows.stream().findFirst();
    }

    public List<AiProblemEntities.Version> findVersionsByDraftId(Long draftId) {
        return jdbcTemplate.query(
                """
                        SELECT id, draft_id, version_no, generation_prompt, assistant_message, llm_response_json, normalized_problem_json, created_at
                        FROM ai_problem_version
                        WHERE draft_id = ?
                        ORDER BY version_no DESC
                        """,
                versionRowMapper,
                draftId
        );
    }

    public void replaceArtifacts(Long versionId, List<AiProblemEntities.Artifact> artifacts) {
        jdbcTemplate.update("DELETE FROM ai_problem_artifact WHERE version_id = ?", versionId);
        for (AiProblemEntities.Artifact artifact : artifacts) {
            insertAndReturnId(
                    """
                            INSERT INTO ai_problem_artifact (
                                version_id, artifact_type, file_name, relative_path, content_type, size_bytes, created_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    versionId,
                    artifact.artifactType(),
                    artifact.fileName(),
                    artifact.relativePath(),
                    artifact.contentType(),
                    artifact.sizeBytes(),
                    artifact.createdAt()
            );
        }
    }

    public List<AiProblemEntities.Artifact> findArtifactsByVersionId(Long versionId) {
        return jdbcTemplate.query(
                """
                        SELECT id, version_id, artifact_type, file_name, relative_path, content_type, size_bytes, created_at
                        FROM ai_problem_artifact
                        WHERE version_id = ?
                        ORDER BY relative_path ASC, id ASC
                        """,
                artifactRowMapper,
                versionId
        );
    }

    private QueryParts buildSessionFilter(String keyword, String status, boolean countOnly) {
        StringBuilder sql = new StringBuilder();
        if (countOnly) {
            sql.append("SELECT COUNT(1) ");
        } else {
            sql.append("""
                    SELECT s.id, s.title, s.created_by, s.provider_key, s.model_name, s.target_rating, s.target_tags, s.status, s.created_at, s.updated_at
                    """);
        }
        sql.append(" FROM ai_problem_session s ");
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null) {
            conditions.add("(LOWER(COALESCE(s.title, '')) LIKE ? OR LOWER(COALESCE(s.target_tags, '')) LIKE ?)");
            params.add(normalizedKeyword);
            params.add(normalizedKeyword);
        }
        String normalizedStatus = normalizeText(status);
        if (normalizedStatus != null) {
            conditions.add("UPPER(COALESCE(s.status, '')) = ?");
            params.add(normalizedStatus.toUpperCase());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return new QueryParts(sql.toString(), params.toArray());
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    private String normalizeKeyword(String keyword) {
        String text = normalizeText(keyword);
        return text == null ? null : "%" + text.toLowerCase() + "%";
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record QueryParts(String sql, Object[] params) {
    }
}
