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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.openrewrite.*;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.LoathingOfOthers;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.quark.Quark;
import org.openrewrite.text.PlainText;

import java.time.ZonedDateTime;
import java.util.List;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.gradle.util.GradleWrapper.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class UpdateGradleWrapper extends Recipe {

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
    String version;

    @Option(displayName = "Distribution type",
            description = "The distribution of Gradle to use. \"bin\" includes Gradle binaries. " +
                    "\"all\" includes Gradle binaries, source code, and documentation. " +
                    "Defaults to \"bin\".",
            valid = {"bin", "all"},
            required = false
    )
    @Nullable
    String distribution;

    @NonFinal
    Validated gradleWrapper;

    @Override
    public Validated validate(ExecutionContext ctx) {
        return super.validate(ctx).and(GradleWrapper.validate(ctx, version, distribution, gradleWrapper));
    }

    //NOTE: Using an explicit constructor here due to a bug that surfaces when running JavaDoc.
    //      See https://github.com/projectlombok/lombok/issues/2372
    @LoathingOfOthers("JavaDoc")
    @JsonCreator
    public UpdateGradleWrapper(String version, @Nullable String distribution) {
        this.version = version;
        this.distribution = distribution;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {

        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext context) {
                return !equalIgnoringSeparators(file.getSourcePath(), WRAPPER_PROPERTIES_LOCATION) ? file :
                        super.visitFile(file, context);
            }

            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
                if (!"distributionUrl".equals(entry.getKey())) {
                    return entry;
                }

                GradleWrapper gradleWrapper = validate(context).getValue();

                // Typical example: https://services.gradle.org/distributions/gradle-7.4-all.zip
                String currentDistributionUrl = entry.getValue().getText();
                if (!gradleWrapper.getPropertiesFormattedUrl().equals(currentDistributionUrl)) {
                    return entry.withMarkers(entry.getMarkers().searchResult());
                }
                return entry;
            }
        };
    }

    @Override
    protected /*~~>*/List<SourceFile> visit(/*~~>*/List<SourceFile> before, ExecutionContext ctx) {
        GradleWrapper gradleWrapper = validate(ctx).getValue();
        assert gradleWrapper != null;

        /*~~>*/List<SourceFile> sourceFileList = ListUtils.map(before, sourceFile -> {
            if (sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                PlainText gradlew = (PlainText) setExecutable(sourceFile);
                String gradlewText = StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew"));
                if (!gradlewText.equals(gradlew.getText())) {
                    gradlew = gradlew.withText(gradlewText);
                }
                return gradlew;
            }
            if (sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                PlainText gradlewBat = (PlainText) setExecutable(sourceFile);
                String gradlewBatText = StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat"));
                if (!gradlewBatText.equals(gradlewBat.getText())) {
                    gradlewBat = gradlewBat.withText(gradlewBatText);
                }
                return gradlewBat;
            }
            if (sourceFile instanceof Properties.File && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                return (Properties.File) new PropertiesVisitor<ExecutionContext>() {
                    @Override
                    public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
                        if (!"distributionUrl".equals(entry.getKey())) {
                            return entry;
                        }
                        return entry.withValue(entry.getValue().withText(gradleWrapper.getPropertiesFormattedUrl()));
                    }
                }.visitNonNull(sourceFile, ctx);
            }
            if (sourceFile instanceof Quark && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                return gradleWrapper.asRemote().withId(sourceFile.getId()).withMarkers(sourceFile.getMarkers());

            }
            return sourceFile;
        });

        return AddGradleWrapper.addGradleFiles(gradleWrapper, sourceFileList);
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
}
