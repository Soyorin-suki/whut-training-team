package com.whut.training.config;

import com.whut.training.service.ProblemCommentMigrationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@DependsOn("liquibase")
public class ProblemCommentMigrationInitializer implements ApplicationRunner {

    private final ProblemCommentMigrationService problemCommentMigrationService;

    public ProblemCommentMigrationInitializer(ProblemCommentMigrationService problemCommentMigrationService) {
        this.problemCommentMigrationService = problemCommentMigrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        problemCommentMigrationService.migrateLegacyComments();
    }
}
