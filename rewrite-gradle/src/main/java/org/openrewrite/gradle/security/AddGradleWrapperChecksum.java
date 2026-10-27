/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.security;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.properties.AddProperty;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.net.URI;
import java.time.Duration;
import java.util.Set;

import static java.util.Collections.singleton;

public class AddGradleWrapperChecksum extends Recipe {

    private static final String DISTRIBUTION_URL_KEY = "distributionUrl";
    private static final String DISTRIBUTION_SHA_256_SUM_KEY = "distributionSha256Sum";

    @Getter
    final String displayName = "Add the Gradle distribution SHA-256 checksum";

    @Getter
    final String description = "Pin the SHA-256 checksum of the Gradle distribution the wrapper already " +
            "downloads, by adding `distributionSha256Sum` to `gradle/wrapper/gradle-wrapper.properties` when it " +
            "is absent. Without it the wrapper runs whatever the distribution URL happens to serve. The sum is " +
            "fetched from the distribution URL with `.sha256` appended, which is how Gradle publishes it, so " +
            "`distributionUrl` is left exactly as it is and the Gradle version does not change. " +
            "`org.openrewrite.gradle.UpdateGradleWrapper` also pins the sum, but as part of moving the wrapper to " +
            "the version it resolves; use this recipe to pin the version already in the file. See the " +
            "[Gradle best practice](https://docs.gradle.org/current/userguide/best_practices_security.html#validate_gradle_checksum).";

    @Getter
    final Set<String> tags = singleton("security");

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(5);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                if (!file.getSourcePath().endsWith(GradleWrapper.WRAPPER_PROPERTIES_LOCATION)) {
                    return file;
                }
                if (!FindProperties.find(file, DISTRIBUTION_SHA_256_SUM_KEY, false).isEmpty()) {
                    return file;
                }

                Set<Properties.Entry> distributionUrl = FindProperties.find(file, DISTRIBUTION_URL_KEY, false);
                if (distributionUrl.size() != 1) {
                    return file;
                }

                String checksum = fetchChecksum(distributionUrl.iterator().next().getValue().getText(), ctx);
                if (checksum == null) {
                    return file;
                }

                // Insert after distributionUrl rather than alphabetically, so the layout matches what
                // UpdateGradleWrapper.generate() writes and the two recipes cannot disagree about one file.
                return (Properties) AddProperty.builder()
                        .property(DISTRIBUTION_SHA_256_SUM_KEY)
                        .value(checksum)
                        .insertMode(AddProperty.InsertMode.After)
                        .insertProperty(DISTRIBUTION_URL_KEY)
                        .build()
                        .getVisitor()
                        .visitNonNull(file, ctx);
            }
        };
    }

    private static @Nullable String fetchChecksum(String propertiesFormattedUrl, ExecutionContext ctx) {
        // The LST holds the value still properties-escaped, which is not a parseable URI.
        String url = propertiesFormattedUrl.replace("\\:", ":");
        if (!url.endsWith(".zip")) {
            return null;
        }
        try {
            return Checksum.fromUri(HttpSenderExecutionContextView.view(ctx).getHttpSender(),
                    URI.create(url + ".sha256")).getHexValue();
        } catch (Exception e) {
            // A mirror that publishes no sum leaves the file unchanged rather than failing the recipe.
            return null;
        }
    }
}
