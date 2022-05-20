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
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.remote.Remote;
import org.openrewrite.remote.RemoteArchive;
import org.openrewrite.text.PlainText;

import java.net.URI;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.GradleProperties.*;

@Value
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
                    "Defaults to \"bin\".",
            example = "bin",
            required = false
    )
    @Nullable
    String distribution;

    @Option(displayName = "Distribution Url",
            description = "The URL to download the Gradle distribution from. Providing the distribution " +
                    "url overrides the \"Version\" and \"Distribution type\" parameters. This is intended " +
                    "to cover customized distributions of the Gradle wrapper.",
            example = "https://services.gradle.org/distributions/gradle-" + DEFAULT_VERSION + "-" + DEFAULT_DISTRIBUTION + ".zip",
            required = false
    )
    @Nullable
    String distributionUrl;

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
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
            } else if(equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                needsGradleWrapperJar = false;
            }
        }
        if(!needsGradleWrapperProperties && !needsGradleWrapperJar && !needsGradleShellScript && !needsGradleBatchScript) {
            return before;
        }

        List<SourceFile> gradleWrapper = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();
        if (needsGradleWrapperProperties) {
            //noinspection UnusedProperty
            Properties.File gradleWrapperProperties = new PropertiesParser().parse(
                            "distributionBase=GRADLE_USER_HOME\n" +
                                    "distributionPath=wrapper/dists\n" +
                                    "distributionUrl=" + getPropertiesFormattedUrl() + "\n" +
                                    "zipStoreBase=GRADLE_USER_HOME\n" +
                                    "zipStorePath=wrapper/dists").get(0)
                    .withSourcePath(WRAPPER_PROPERTIES_LOCATION);
            gradleWrapper.add(gradleWrapperProperties);
        }
        FileAttributes wrapperScriptAttributes = new FileAttributes(now, now, now, true, true, true, 1L);
        if (needsGradleShellScript) {
            PlainText gradlew = new PlainText(randomId(), WRAPPER_SCRIPT_LOCATION, Markers.EMPTY, null, false,
                    wrapperScriptAttributes, null,
                    StringUtils.readFully(AddGradleWrapper.class.getResourceAsStream("/gradlew")));
            gradleWrapper.add(gradlew);
        }

        if (needsGradleBatchScript) {
            PlainText gradlewBat = new PlainText(randomId(), WRAPPER_BATCH_LOCATION, Markers.EMPTY,null, false,
                    wrapperScriptAttributes, null,
                    StringUtils.readFully(AddGradleWrapper.class.getResourceAsStream("/gradlew.bat")));
            gradleWrapper.add(gradlewBat);
        }

        if (needsGradleWrapperJar) {
            RemoteArchive gradleWrapperJar = Remote.builder(WRAPPER_JAR_LOCATION, URI.create(getDistributionUrl()))
                    .build(Paths.get("gradle-" + getVersion() + "/lib/gradle-wrapper-" + getVersion() + ".jar"));
            gradleWrapper.add(gradleWrapperJar);
        }

        return ListUtils.concatAll(
                before,
                gradleWrapper
        );
    }

    private String getVersion() {
        if(version == null) {
            return DEFAULT_VERSION;
        }
        return version;
    }

    private String getDistributionUrl() {
        if(distributionUrl == null) {
            String desiredDistribution = (distribution == null) ? DEFAULT_DISTRIBUTION : distribution;
            return "https://services.gradle.org/distributions/gradle-" + getVersion() + "-" + desiredDistribution + ".zip";
        }
        return distributionUrl;
    }

    private String getPropertiesFormattedUrl() {
        return getDistributionUrl().replaceAll("(?<!\\\\)://", "\\\\://");
    }
}
