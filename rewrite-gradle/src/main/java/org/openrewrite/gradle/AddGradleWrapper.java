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
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainText;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.util.GradleWrapper.*;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class AddGradleWrapper extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add Gradle wrapper";
    }

    @Override
    public String getDescription() {
        return "Add a Gradle wrapper where one does not exist.";
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
    public AddGradleWrapper(String version, String distribution) {
        this.version = version;
        this.distribution = distribution;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new HasSourcePath<>("regex", ".+\\.gradle(\\.kts)?$");
    }

    @Override
    protected /*~~>*/List<SourceFile> visit(/*~~>*/List<SourceFile> before, ExecutionContext ctx) {
        GradleWrapper gradleWrapper = validate(ctx).getValue();
        assert gradleWrapper != null;
        return addGradleFiles(gradleWrapper, before);
    }

    public static /*~~>*/List<SourceFile> addGradleFiles(GradleWrapper gradleWrapper, /*~~>*/List<SourceFile> before) {
        boolean needsGradleWrapperProperties = true;
        boolean needsGradleWrapperJar = true;
        boolean needsGradleShellScript = true;
        boolean needsGradleBatchScript = true;
        for (SourceFile sourceFile : before) {
            if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                needsGradleWrapperProperties = false;
            } else if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                needsGradleShellScript = false;
            } else if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                needsGradleBatchScript = false;
            } else if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                needsGradleWrapperJar = false;
            }
        }
        if (!needsGradleWrapperProperties && !needsGradleWrapperJar && !needsGradleShellScript && !needsGradleBatchScript) {
            return before;
        }

        /*~~>*/List<SourceFile> gradleWrapperFiles = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();
        if (needsGradleWrapperProperties) {
            //noinspection UnusedProperty
            Properties.File gradleWrapperProperties = new PropertiesParser().parse(
                            "distributionBase=GRADLE_USER_HOME\n" +
                                    "distributionPath=wrapper/dists\n" +
                                    "distributionUrl=" + gradleWrapper.getPropertiesFormattedUrl() + "\n" +
                                    "zipStoreBase=GRADLE_USER_HOME\n" +
                                    "zipStorePath=wrapper/dists").get(0)
                    .withSourcePath(WRAPPER_PROPERTIES_LOCATION);
            gradleWrapperFiles.add(gradleWrapperProperties);
        }
        FileAttributes wrapperScriptAttributes = new FileAttributes(now, now, now, true, true, true, 1L);
        if (needsGradleShellScript) {
            PlainText gradlew = new PlainText(randomId(), WRAPPER_SCRIPT_LOCATION, Markers.EMPTY, null, false,
                    wrapperScriptAttributes, null,
                    StringUtils.readFully(AddGradleWrapper.class.getResourceAsStream("/gradlew")));
            gradleWrapperFiles.add(gradlew);
        }

        if (needsGradleBatchScript) {
            PlainText gradlewBat = new PlainText(randomId(), WRAPPER_BATCH_LOCATION, Markers.EMPTY, null, false,
                    wrapperScriptAttributes, null,
                    StringUtils.readFully(AddGradleWrapper.class.getResourceAsStream("/gradlew.bat")));
            gradleWrapperFiles.add(gradlewBat);
        }

        if (needsGradleWrapperJar) {
            gradleWrapperFiles.add(gradleWrapper.asRemote());
        }

        return ListUtils.concatAll(
                before,
                gradleWrapperFiles
        );
    }
}
