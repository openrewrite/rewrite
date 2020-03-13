/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.git;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.openrewrite.Change;
import org.openrewrite.ChangePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GithubChangePublisher implements ChangePublisher {
    private static final Logger logger = LoggerFactory.getLogger(GithubChangePublisher.class);

    private final GitHub github;
    private final String organization;
    private final String repository;
    private final String commitMessage;
    private final boolean verifyOriginal;

    public GithubChangePublisher(GitHub github, String organization, String repository, String commitMessage) {
        this(github, organization, repository, commitMessage, false);
    }

    public GithubChangePublisher(GitHub github, String organization, String repository, String commitMessage, boolean verifyOriginal) {
        this.github = github;
        this.organization = organization;
        this.repository = repository;
        this.commitMessage = commitMessage;
        this.verifyOriginal = verifyOriginal;
    }

    @Override
    public boolean publish(Change<?> change) {
        Timer.Sample sample = Timer.start();
        Timer.Builder timer = Timer.builder("rewrite.publish.github")
                .description("Individual source file changes published directly to the GitHub 'Create or update a file' API")
                .tag("organization", organization)
                .tag("repository", repository)
                .tag("file.type", change.getFixed().getFileType());

        try {
            GHContent fileContent = github.getRepository(organization + "/" + repository)
                    .getFileContent(change.getFixed().getSourcePath());

            if (verifyOriginal &&
                    change.getOriginal() != null &&
                    !change.getOriginal().print().equals(new String(fileContent.read().readAllBytes(), StandardCharsets.UTF_8))) {
                logger.warn("Attempting to make a change to " + organization +
                        "/" + repository + ":" + change.getOriginal().getSourcePath() +
                        " in repository, but the contents in GitHub do not match the original source");
                sample.stop(timer.tag("outcome", "Original not up-to-date").register(Metrics.globalRegistry));
                return false;
            }

            fileContent.update(change.getFixed().getSourcePath(), commitMessage);
            logger.info("Published change " + organization + "/" + repository + ":" + change.getFixed().getSourcePath() + " to GitHub");
            sample.stop(timer.tag("outcome", "Success").register(Metrics.globalRegistry));
            return true;
        } catch (IOException e) {
            logger.warn("Unable to connect to GitHub repository " + organization + "/" + repository);
            sample.stop(timer.tag("outcome", "Failed to connect to repository").register(Metrics.globalRegistry));
            return false;
        }
    }
}
