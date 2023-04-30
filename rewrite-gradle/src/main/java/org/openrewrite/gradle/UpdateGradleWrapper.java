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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.gradle.util.GradleWrapper.*;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class UpdateGradleWrapper extends ScanningRecipe<UpdateGradleWrapper.GradleWrapperState> {

    @Override
    public String getDisplayName() {
        return "Update Gradle wrapper";
    }

    @Override
    public String getDescription() {
        return "Update the version of Gradle used in an existing Gradle wrapper.";
    }

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "7.x")
    final String version;

    @Option(displayName = "Distribution type",
            description = "The distribution of Gradle to use. \"bin\" includes Gradle binaries. " +
                          "\"all\" includes Gradle binaries, source code, and documentation. " +
                          "Defaults to \"bin\".",
            valid = {"bin", "all"},
            required = false
    )
    @Nullable
    final String distribution;

    @Option(displayName = "Repository URL",
            description = "The URL of the repository to download the Gradle distribution from. Currently only supports " +
                          "repositories like services.gradle.org, not arbitrary maven or ivy repositories. " +
                          "Defaults to `https://services.gradle.org/versions/all`.",
            example = "https://services.gradle.org/versions/all")
    @Nullable
    final String repositoryUrl;

    @NonFinal
    transient Validated gradleWrapperValidation;

    @Override
    public Validated validate(ExecutionContext ctx) {
        gradleWrapperValidation = GradleWrapper.validate(ctx,
                isBlank(version) ? "latest.release" : version,
                distribution, gradleWrapperValidation, repositoryUrl);
        return super.validate(ctx).and(gradleWrapperValidation);
    }

    static class GradleWrapperState {
        boolean needsWrapperUpdate = true;
        boolean needsGradleWrapperProperties = true;
        boolean needsGradleWrapperJar = true;
        boolean needsGradleShellScript = true;
        boolean needsGradleBatchScript = true;
    }

    @Override
    public GradleWrapperState getInitialValue() {
        return new GradleWrapperState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(GradleWrapperState acc) {
        return Preconditions.firstAcceptable(
                new PropertiesVisitor<ExecutionContext>() {
                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
                        if (super.isAcceptable(sourceFile, executionContext) &&
                            equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                            acc.needsGradleWrapperProperties = false;
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                        if (!"distributionUrl".equals(entry.getKey())) {
                            return entry;
                        }

                        GradleWrapper gradleWrapper = requireNonNull(validate(ctx).getValue());

                        // Typical example: https://services.gradle.org/distributions/gradle-7.4-all.zip
                        String currentDistributionUrl = entry.getValue().getText();
                        if (gradleWrapper.getPropertiesFormattedUrl().equals(currentDistributionUrl)) {
                            acc.needsWrapperUpdate = false;
                        }
                        return entry;
                    }
                }
        );
    }

    @Override
    public Collection<SourceFile> generate(GradleWrapperState acc, ExecutionContext ctx) {
        if (!acc.needsWrapperUpdate) {
            return Collections.emptyList();
        }

        List<SourceFile> gradleWrapperFiles = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();

        GradleWrapper gradleWrapper = requireNonNull(gradleWrapperValidation.getValue());

        if (acc.needsGradleWrapperProperties) {
            //noinspection UnusedProperty
            Properties.File gradleWrapperProperties = new PropertiesParser().parse(
                            "distributionBase=GRADLE_USER_HOME\n" +
                            "distributionPath=wrapper/dists\n" +
                            "distributionUrl=" + gradleWrapper.getPropertiesFormattedUrl() + "\n" +
                            "distributionSha256Sum=" + gradleWrapper.getDistributionChecksum().getHexValue() + "\n" +
                            "zipStoreBase=GRADLE_USER_HOME\n" +
                            "zipStorePath=wrapper/dists")
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as properties"))
                    .withSourcePath(WRAPPER_PROPERTIES_LOCATION);
            gradleWrapperFiles.add(gradleWrapperProperties);
        }
        FileAttributes wrapperScriptAttributes = new FileAttributes(now, now, now, true, true, true, 1L);
        if (acc.needsGradleShellScript) {
            PlainText gradlew = PlainText.builder()
                    .text(StringUtils.readFully(requireNonNull(UpdateGradleWrapper.class.getResourceAsStream("/gradlew"))))
                    .sourcePath(WRAPPER_SCRIPT_LOCATION)
                    .fileAttributes(wrapperScriptAttributes)
                    .build();
            gradleWrapperFiles.add(gradlew);
        }

        if (acc.needsGradleBatchScript) {
            PlainText gradlewBat = PlainText.builder()
                    .text(StringUtils.readFully(requireNonNull(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat"))))
                    .sourcePath(WRAPPER_BATCH_LOCATION)
                    .fileAttributes(wrapperScriptAttributes)
                    .build();
            gradleWrapperFiles.add(gradlewBat);
        }

        if (acc.needsGradleWrapperJar) {
            gradleWrapperFiles.add(gradleWrapper.asRemote());
        }

        return gradleWrapperFiles;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(GradleWrapperState acc) {
        if (!acc.needsWrapperUpdate) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            final GradleWrapper gradleWrapper = requireNonNull(gradleWrapperValidation.getValue());

            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) tree;

                if (sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                    PlainText gradlew = (PlainText) setExecutable(sourceFile);
                    String gradlewText = StringUtils.readFully(requireNonNull(UpdateGradleWrapper.class.getResourceAsStream("/gradlew")),
                            sourceFile.getCharset() == null ? StandardCharsets.UTF_8 : sourceFile.getCharset());
                    if (!gradlewText.equals(gradlew.getText())) {
                        gradlew = gradlew.withText(gradlewText);
                    }
                    return gradlew;
                }
                if (sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                    PlainText gradlewBat = (PlainText) setExecutable(sourceFile);
                    String gradlewBatText = StringUtils.readFully(requireNonNull(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat")),
                            sourceFile.getCharset() == null ? StandardCharsets.UTF_8 : sourceFile.getCharset());
                    if (!gradlewBatText.equals(gradlewBat.getText())) {
                        gradlewBat = gradlewBat.withText(gradlewBatText);
                    }
                    return gradlewBat;
                }
                if (sourceFile instanceof Properties.File && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                    return new WrapperPropertiesVisitor(gradleWrapper).visitNonNull(sourceFile, ctx);
                }
                if (sourceFile instanceof Quark && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                    return gradleWrapper.asRemote().withId(sourceFile.getId()).withMarkers(sourceFile.getMarkers());
                }
                return sourceFile;
            }
        };
    }


    private static <T extends SourceFile> T setExecutable(T sourceFile) {
        FileAttributes attributes = sourceFile.getFileAttributes();
        if (attributes == null) {
            ZonedDateTime now = ZonedDateTime.now();
            return sourceFile.withFileAttributes(new FileAttributes(now, now, now, true, true, true, 1));
        } else {
            return sourceFile.withFileAttributes(sourceFile.getFileAttributes().withExecutable(true));
        }
    }

    private static class WrapperPropertiesVisitor extends PropertiesVisitor<ExecutionContext> {

        private static final String DISTRIBUTION_SHA_256_SUM_KEY = "distributionSha256Sum";
        private final GradleWrapper gradleWrapper;

        public WrapperPropertiesVisitor(GradleWrapper gradleWrapper) {
            this.gradleWrapper = gradleWrapper;
        }

        @Override
        public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
            Properties p = super.visitFile(file, executionContext);
            Set<Properties.Entry> properties = FindProperties.find(p, DISTRIBUTION_SHA_256_SUM_KEY, false);
            if (properties.isEmpty()) {
                Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, gradleWrapper.getDistributionChecksum().getHexValue());
                Properties.Entry entry = new Properties.Entry(Tree.randomId(), "\n", Markers.EMPTY, DISTRIBUTION_SHA_256_SUM_KEY, "", Properties.Entry.Delimiter.EQUALS, propertyValue);
                List<Properties.Content> contentList = ListUtils.concat(((Properties.File) p).getContent(), entry);
                p = ((Properties.File) p).withContent(contentList);
            }
            return p;
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
            if ("distributionUrl".equals(entry.getKey())) {
                return entry.withValue(entry.getValue().withText(gradleWrapper.getPropertiesFormattedUrl()));
            }
            if (DISTRIBUTION_SHA_256_SUM_KEY.equals(entry.getKey())) {
                return entry.withValue(entry.getValue().withText(gradleWrapper.getDistributionChecksum().getHexValue()));
            }
            return entry;
        }
    }
}
