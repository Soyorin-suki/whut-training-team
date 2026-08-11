package com.whut.training.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 趣味签到签文目录。
 *
 * <p>内容从独立 JSON 资源加载，业务逻辑不再内嵌签文。目录随应用发布，
 * 因此不依赖第三方接口也能稳定签到；以后可以无缝替换为数据库目录。
 */
@Component
public class FortuneCatalog {

    private final List<Fortune> fortunes;

    public FortuneCatalog(
            ObjectMapper objectMapper,
            @Value("${fortune.catalog-location:classpath:fortunes.json}") Resource catalogResource
    ) {
        try (InputStream input = catalogResource.getInputStream()) {
            List<Fortune> loaded = objectMapper.readValue(input, new TypeReference<>() { });
            if (loaded == null || loaded.isEmpty()) {
                throw new IllegalStateException("fortune catalog must not be empty");
            }
            loaded.forEach(FortuneCatalog::validate);
            Set<String> uniqueKeys = loaded.stream()
                    .map(Fortune::key)
                    .collect(Collectors.toSet());
            if (uniqueKeys.size() != loaded.size()) {
                throw new IllegalStateException("fortune catalog contains duplicate keys");
            }
            this.fortunes = List.copyOf(loaded);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load fortune catalog", ex);
        }
    }

    public Fortune select(Long userId, LocalDate date) {
        int seed = Objects.hash(userId, date.toEpochDay(), "WHUT-ACM-FORTUNE");
        return fortunes.get(Math.floorMod(seed, fortunes.size()));
    }

    int size() {
        return fortunes.size();
    }

    private static void validate(Fortune fortune) {
        if (fortune == null
                || isBlank(fortune.key())
                || isBlank(fortune.title())
                || isBlank(fortune.message())
                || isBlank(fortune.luckyTag())
                || isBlank(fortune.color())
                || fortune.level() < 1
                || fortune.level() > 5) {
            throw new IllegalStateException("fortune catalog contains an invalid item");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Fortune(
            String key,
            String title,
            String message,
            String luckyTag,
            String color,
            int level
    ) {
    }
}
