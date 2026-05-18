package com.whut.training.service;

import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiProblemValidator {

    private static final int MIN_RATING = 800;
    private static final int MAX_RATING = 3500;

    public List<String> normalizeTags(List<String> tags, String fieldName, boolean required) {
        List<String> normalized = new ArrayList<>();
        if (tags != null) {
            Set<String> unique = new LinkedHashSet<>();
            for (String tag : tags) {
                String text = normalizeText(tag);
                if (text != null) {
                    unique.add(text);
                }
            }
            normalized.addAll(unique);
        }
        if (required && normalized.isEmpty()) {
            throw new BusinessException(400, fieldName + " must not be empty");
        }
        return normalized;
    }

    public AiProblemDtos.ProblemContent normalizeProblemContent(AiProblemDtos.ProblemContent source) {
        if (source == null) {
            throw new BusinessException(400, "problem content is required");
        }

        String title = requireText(source.title(), "title");
        String statementMd = requireText(source.statementMd(), "statementMd");
        String inputSpecMd = requireText(source.inputSpecMd(), "inputSpecMd");
        String outputSpecMd = requireText(source.outputSpecMd(), "outputSpecMd");
        String constraintMd = requireText(source.constraintMd(), "constraintMd");
        String hintMd = normalizeText(source.hintMd());
        Integer rating = normalizeRating(source.rating());
        List<String> tags = normalizeTags(source.tags(), "tags", true);
        String checkerNoteMd = normalizeText(source.checkerNoteMd());
        String testPlanMd = requireText(source.testPlanMd(), "testPlanMd");
        String originalityNotice = requireText(source.originalityNotice(), "originalityNotice");

        List<AiProblemDtos.SampleItem> samples = normalizeSamples(source.samples());
        List<AiProblemDtos.TestCaseItem> tests = normalizeTests(source.tests());

        return new AiProblemDtos.ProblemContent(
                title,
                statementMd,
                inputSpecMd,
                outputSpecMd,
                constraintMd,
                hintMd,
                rating,
                tags,
                checkerNoteMd,
                samples,
                testPlanMd,
                tests,
                originalityNotice
        );
    }

    public AiProblemDtos.ProblemContent mergePatch(AiProblemDtos.ProblemContent current, AiProblemDtos.DraftPatchRequest patch) {
        if (current == null) {
            throw new BusinessException(404, "draft content not found");
        }
        if (patch == null) {
            throw new BusinessException(400, "request body is invalid or missing");
        }

        return normalizeProblemContent(new AiProblemDtos.ProblemContent(
                firstNonBlank(patch.title(), current.title()),
                firstNonBlank(patch.statementMd(), current.statementMd()),
                firstNonBlank(patch.inputSpecMd(), current.inputSpecMd()),
                firstNonBlank(patch.outputSpecMd(), current.outputSpecMd()),
                firstNonBlank(patch.constraintMd(), current.constraintMd()),
                patch.hintMd() == null ? current.hintMd() : patch.hintMd(),
                patch.rating() == null ? current.rating() : patch.rating(),
                patch.tags() == null ? current.tags() : patch.tags(),
                patch.checkerNoteMd() == null ? current.checkerNoteMd() : patch.checkerNoteMd(),
                patch.samples() == null ? current.samples() : patch.samples(),
                firstNonBlank(patch.testPlanMd(), current.testPlanMd()),
                patch.tests() == null ? current.tests() : patch.tests(),
                firstNonBlank(patch.originalityNotice(), current.originalityNotice())
        ));
    }

    private List<AiProblemDtos.SampleItem> normalizeSamples(List<AiProblemDtos.SampleItem> samples) {
        if (samples == null || samples.isEmpty()) {
            throw new BusinessException(400, "samples must not be empty");
        }
        List<AiProblemDtos.SampleItem> normalized = new ArrayList<>();
        for (AiProblemDtos.SampleItem sample : samples) {
            if (sample == null) {
                throw new BusinessException(400, "samples must not contain null values");
            }
            normalized.add(new AiProblemDtos.SampleItem(
                    requireText(sample.input(), "sample input"),
                    requireText(sample.output(), "sample output"),
                    normalizeText(sample.explanation())
            ));
        }
        return normalized;
    }

    private List<AiProblemDtos.TestCaseItem> normalizeTests(List<AiProblemDtos.TestCaseItem> tests) {
        if (tests == null || tests.isEmpty()) {
            throw new BusinessException(400, "tests must not be empty");
        }
        List<AiProblemDtos.TestCaseItem> normalized = new ArrayList<>();
        int index = 1;
        for (AiProblemDtos.TestCaseItem testCase : tests) {
            if (testCase == null) {
                throw new BusinessException(400, "tests must not contain null values");
            }
            String name = normalizeText(testCase.name());
            if (name == null) {
                name = "test-" + index;
            }
            normalized.add(new AiProblemDtos.TestCaseItem(
                    name,
                    requireText(testCase.input(), "test input"),
                    requireText(testCase.output(), "test output")
            ));
            index++;
        }
        return normalized;
    }

    private Integer normalizeRating(Integer rating) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessException(400, "rating must be between 800 and 3500");
        }
        return rating;
    }

    private String requireText(String value, String fieldName) {
        String text = normalizeText(value);
        if (text == null) {
            throw new BusinessException(400, fieldName + " must not be blank");
        }
        return text;
    }

    private String firstNonBlank(String preferred, String fallback) {
        String text = normalizeText(preferred);
        return text == null ? fallback : text;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
