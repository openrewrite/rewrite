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
import org.openrewrite.binary.Binary;
import org.openrewrite.binary.BinaryParser;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainText;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpdateGradleWrapper extends Recipe {
    private static final String DEFAULT_VERSION = "7.4.2";
    private static final Path WRAPPER_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.properties");

    @Override
    public String getDisplayName() {
        return "Update Gradle wrapper";
    }

    @Override
    public String getDescription() {
        return "Update the version of Gradle used in an existing Gradle Wrapper.";
    }

    @Option(displayName = "Version",
            description = "The version of Gradle to use. Defaults to " + DEFAULT_VERSION,
            example = "7.4.2",
            required = false
    )
    @Nullable
    String version;

    @Option(displayName = "Distribution type",
            description = "The distribution of Gradle to use. \"bin\" includes Gradle binaries. " +
                    "\"all\" includes Gradle binaries, source code, and documentation. " +
                    "If not specified the existing distribution type will be used.",
            example = "bin",
            required = false
    )
    @Nullable
    String distribution;

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext context) {
                if (!PathUtils.equalIgnoringSeparators(file.getSourcePath(), WRAPPER_LOCATION)) {
                    return file;
                }
                return super.visitFile(file, context);
            }

            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
                if (!"distributionUrl".equals(entry.getKey())) {
                    return entry;
                }
                // Typical example: https\://services.gradle.org/distributions/gradle-7.4-all.zip
                String distributionUrl = entry.getValue().getText();
                String[] distributionComponents = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1).split("-");
                if (distributionComponents.length != 3) {
                    return entry;
                }
                String desiredVersion = (version == null) ? DEFAULT_VERSION : version;
                String currentVersion = distributionComponents[1];
                if (!desiredVersion.equals(currentVersion)) {
                    return entry.withMarkers(entry.getMarkers().searchResult());
                }
                if(distribution == null) {
                    return entry;
                }
                String currentDistribution = distributionComponents[2].substring(0, distributionComponents[2].indexOf('.'));
                if (!distribution.equals(currentDistribution)) {
                    return entry.withMarkers(entry.getMarkers().searchResult());
                }

                return entry;
            }
        };
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        PlainText gradlew = new PlainText(randomId(),
                Paths.get("gradlew"), Markers.EMPTY,
                StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew")));
        PlainText gradlewBat = new PlainText(randomId(),
                Paths.get("gradlew.bat"), Markers.EMPTY,
                StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat")));
        Binary gradleWrapperJar = new BinaryParser().parseInputs(singletonList(
                new Parser.Input(Paths.get("gradle/wrapper/gradle-wrapper.jar"),
                        () -> UpdateGradleWrapper.class.getResourceAsStream("/gradle-wrapper.jar"))), null, ctx).get(0);

        return ListUtils.concatAll(
                ListUtils.map(before, sourceFile -> {
                    if (!(sourceFile instanceof Properties)) {
                        return sourceFile;
                    }
                    return (Properties.File) new UpdateWrapperPropsVisitor().visitNonNull(sourceFile, ctx);
                }),
                Arrays.asList(gradlew, gradlewBat, gradleWrapperJar)
        );
    }

    private class UpdateWrapperPropsVisitor extends PropertiesVisitor<ExecutionContext> {
        @Override
        public Properties visitFile(Properties.File file, ExecutionContext context) {
            if (!PathUtils.equalIgnoringSeparators(file.getSourcePath(), WRAPPER_LOCATION)) {
                return file;
            }
            return super.visitFile(file, context);
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext context) {
            if (!"distributionUrl".equals(entry.getKey())) {
                return entry;
            }
            String distributionUrl = entry.getValue().getText();
            String desiredVersion = (version == null) ? DEFAULT_VERSION : version;

            String desiredDistributionUrl;
            if(distribution == null) {
                desiredDistributionUrl = distributionUrl.substring(0, distributionUrl.lastIndexOf('/') + 1) +
                        "gradle-" + desiredVersion + distributionUrl.substring(distributionUrl.lastIndexOf('-'));
            } else {
                desiredDistributionUrl = distributionUrl.substring(0, distributionUrl.lastIndexOf('/') + 1) +
                        "gradle-" + desiredVersion + "-" + distribution + ".zip";
            }
            return entry.withValue(entry.getValue().withText(desiredDistributionUrl));
        }
    }
}
