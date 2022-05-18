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
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainText;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.PathUtils.equalIgnoringSeparators;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpdateGradleWrapper extends Recipe {
    private static final String DEFAULT_VERSION = "7.4.2";
    private static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.properties");
    private static final Path WRAPPER_SCRIPT_LOCATION = Paths.get("gradlew");
    private static final Path WRAPPER_BATCH_LOCATION = Paths.get("gradlew.bat");
    private static final Path WRAPPER_JAR_LOCATION = Paths.get("gradle/wrapper/gradle-wrapper.jar");

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
            example = DEFAULT_VERSION,
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
                // Typical example: https\://services.gradle.org/distributions/gradle-7.4-all.zip
                String currentDistributionUrl = entry.getValue().getText();
                if (distributionUrl != null && (!distributionUrl.equals(currentDistributionUrl)
                        || !distributionUrl.equals(currentDistributionUrl.replace("https\\://", "https://")))) {
                    return entry.withMarkers(entry.getMarkers().searchResult());
                }

                String[] distributionComponents = currentDistributionUrl.substring(currentDistributionUrl.lastIndexOf('/') + 1).split("-");
                if (distributionComponents.length != 3) {
                    return entry;
                }
                String desiredVersion = (version == null) ? DEFAULT_VERSION : version;
                String currentVersion = distributionComponents[1];
                if (!desiredVersion.equals(currentVersion)) {
                    return entry.withMarkers(entry.getMarkers().searchResult());
                }
                if (distribution == null) {
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
        URL wrapperJarUrl = AddGradleWrapper.class.getResource("/gradle-wrapper.jar.dontunpack");
        FileAttributes wrapperJarAttributes = wrapperJarUrl != null ? FileAttributes.fromPath(Paths.get(wrapperJarUrl.getPath())) : null;
        Binary gradleWrapperJar = new BinaryParser().parseInputs(singletonList(
                new Parser.Input(Paths.get("gradle/wrapper/gradle-wrapper.jar"), wrapperJarAttributes,
                        () -> UpdateGradleWrapper.class.getResourceAsStream("/gradle-wrapper.jar.dontunpack"))),null, ctx).get(0);

        return ListUtils.concat(
                ListUtils.map(before, sourceFile -> {
                    if(sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                        PlainText gradlew = (PlainText)sourceFile;
                        String gradlewText = StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew"));
                        if(!gradlewText.equals(gradlew.getText())) {
                            gradlew = gradlew.withText(gradlewText);
                        }
                        return gradlew;
                    }
                    if(sourceFile instanceof PlainText && equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                        PlainText gradlewBat = (PlainText)sourceFile;
                        String gradlewBatText = StringUtils.readFully(UpdateGradleWrapper.class.getResourceAsStream("/gradlew.bat"));
                        if(!gradlewBatText.equals(gradlewBat.getText())) {
                            gradlewBat = gradlewBat.withText(gradlewBatText);
                        }
                        return gradlewBat;
                    }
                    if (sourceFile instanceof Properties.File) {
                        return (Properties.File) new UpdateWrapperPropsVisitor().visitNonNull(sourceFile, ctx);
                    }
                    return sourceFile;
                }),
                gradleWrapperJar
        );
    }

    private class UpdateWrapperPropsVisitor extends PropertiesVisitor<ExecutionContext> {
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

            String desiredDistributionUrl = getDesiredDistributionUrl(entry.getValue().getText());
            return entry.withValue(entry.getValue().withText(desiredDistributionUrl));
        }

        private String getDesiredDistributionUrl(String currentDistributionUrl) {
            if (distributionUrl != null) {
                return distributionUrl.replaceAll("(?<!\\\\)://", "\\\\://");
            }

            String desiredVersion = (version == null) ? DEFAULT_VERSION : version;

            String desiredDistributionUrl;
            if (distribution == null) {
                desiredDistributionUrl = currentDistributionUrl.substring(0, currentDistributionUrl.lastIndexOf('/') + 1) +
                        "gradle-" + desiredVersion + currentDistributionUrl.substring(currentDistributionUrl.lastIndexOf('-'));
            } else {
                desiredDistributionUrl = currentDistributionUrl.substring(0, currentDistributionUrl.lastIndexOf('/') + 1) +
                        "gradle-" + desiredVersion + "-" + distribution + ".zip";
            }
            return desiredDistributionUrl;
        }
    }
}
