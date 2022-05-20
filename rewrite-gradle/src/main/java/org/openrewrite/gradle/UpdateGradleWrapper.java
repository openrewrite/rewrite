/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.text.PlainText;

import java.net.URI;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.gradle.GradleProperties.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpdateGradleWrapper extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update Gradle wrapper";
    }

    @Override
    public String getDescription() {
        return "Update the version of Gradle used in an existing Gradle Wrapper.";
    }

    @Option(displayName = "Distribution Url",
            description = "The URL to download the Gradle distribution from. Providing the distribution " +
                    "url overrides the \"Version\" and \"Distribution type\" parameters. This is intended " +
                    "to cover customized distributions of the Gradle wrapper.",
            example = "https://services.gradle.org/distributions/gradle-" + DEFAULT_VERSION + "-bin.zip",
            required = false
    )
    @Nullable
    String distributionUrl;

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext context) {
                if (!equalIgnoringSeparators(file.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                    return file;
                }

                return super.visitFile(file, context);
            }

            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
                if (!"distributionUrl".equals(entry.getKey())) {
                    return entry;
                }
                // Typical example: https://services.gradle.org/distributions/gradle-7.4-all.zip
                String currentDistributionUrl = entry.getValue().getText();
                if (!getDistributionUrl().equals(currentDistributionUrl)) {
                    return entry.withMarkers(entry.getMarkers().searchResult());
                }
                return entry;
            }
        };
    }

    private String getDistributionUrl() {
        if(distributionUrl == null) {
            return "https://services.gradle.org/distributions/gradle-" + DEFAULT_VERSION + "-" + DEFAULT_DISTRIBUTION + ".zip";
        }
        return distributionUrl;
    }

    private static final Pattern VERSION_EXTRACTING_PATTERN = Pattern.compile("/gradle-([-a-zA-Z\\d.+]+)-[a-zA-Z]+.zip");
    private String getVersion() {
        if(distributionUrl == null) {
            return DEFAULT_VERSION;
        }
        Matcher m = VERSION_EXTRACTING_PATTERN.matcher(getDistributionUrl());
        if(m.find()) {
            return m.group(1);
        }
        throw new RuntimeException("Could not determine version for distributionURL: " + getDistributionUrl());
    }

    private String getPropertiesFormattedUrl() {
        return getDistributionUrl().replaceAll("(?<!\\\\)://", "\\\\://");
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return ListUtils.map(before, sourceFile -> {
                    if(sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                        PlainText gradlew = (PlainText) setExecutable(sourceFile);
                        String gradlewText = StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew"));
                        if(!gradlewText.equals(gradlew.getText())) {
                            gradlew = gradlew.withText(gradlewText);
                        }
                        return gradlew;
                    }
                    if(sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                        PlainText gradlewBat = (PlainText) setExecutable(sourceFile);
                        String gradlewBatText = StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat"));
                        if(!gradlewBatText.equals(gradlewBat.getText())) {
                            gradlewBat = gradlewBat.withText(gradlewBatText);
                        }
                        return gradlewBat;
                    }
                    if (sourceFile instanceof Properties.File && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                        return (Properties.File) new UpdateWrapperPropsVisitor().visitNonNull(sourceFile, ctx);
                    }
                    if(sourceFile instanceof Quark && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                        return Remote.builder(WRAPPER_JAR_LOCATION, URI.create(getDistributionUrl()))
                                .build(Paths.get("gradle-" + getVersion() + "/lib/gradle-wrapper-" + getVersion() + ".jar"))
                                .withId(sourceFile.getId());
                    }
                    return sourceFile;
                });
    }

    private class UpdateWrapperPropsVisitor extends PropertiesVisitor<ExecutionContext> {
        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
            if (!"distributionUrl".equals(entry.getKey())) {
                return entry;
            }
            return entry.withValue(entry.getValue().withText(getPropertiesFormattedUrl()));
        }
    }

    private static <T extends SourceFile> T setExecutable(T sourceFile) {
        FileAttributes attributes = sourceFile.getFileAttributes();
        if(attributes == null) {
            ZonedDateTime now = ZonedDateTime.now();
            return sourceFile.withFileAttributes(new FileAttributes(now, now, now, true, true, true, 1));
        } else {
            return sourceFile.withFileAttributes(sourceFile.getFileAttributes().withExecutable(true));
        }
    }
}
