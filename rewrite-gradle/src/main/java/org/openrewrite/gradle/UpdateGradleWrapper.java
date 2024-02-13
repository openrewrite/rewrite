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
import lombok.Getter;
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
import org.openrewrite.remote.Remote;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.text.PlainText;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.gradle.util.GradleWrapper.*;
import static org.openrewrite.internal.StringUtils.formatUriForPropertiesFile;
import static org.openrewrite.internal.StringUtils.isBlank;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class UpdateGradleWrapper extends ScanningRecipe<UpdateGradleWrapper.GradleWrapperState> {

    @Override
    public String getDisplayName() {
        return "Update Gradle wrapper";
    }

    @Override
    public String getDescription() {
        return "Update the version of Gradle used in an existing Gradle wrapper. " +
               "Queries services.gradle.org to determine the available releases, but prefers the artifact repository URL " +
               "which already exists within the wrapper properties file. " +
               "If your artifact repository does not contain the same Gradle distributions as services.gradle.org, " +
               "then the recipe may suggest a version which is not available in your artifact repository.";
    }

    @Getter
    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "Defaults to the latest release available from services.gradle.org if not specified.",
            example = "7.x",
            required = false)
    @Nullable
    final String version;

    @Getter
    @Option(displayName = "Distribution type",
            description = "The distribution of Gradle to use. \"bin\" includes Gradle binaries. " +
                          "\"all\" includes Gradle binaries, source code, and documentation. " +
                          "Defaults to \"bin\".",
            valid = {"bin", "all"},
            required = false
    )
    @Nullable
    final String distribution;

    @Getter
    @Option(displayName = "Add if missing",
            description = "Add a Gradle wrapper, if it's missing. Defaults to `true`.",
            required = false)
    @Nullable
    final Boolean addIfMissing;

    @Getter
    @Option(displayName = "Wrapper URI",
            description = "The URI of the Gradle wrapper distribution. " +
                          "Lookup of available versions still requires access to https://services.gradle.org " +
                          "When this is specified the exact literal values supplied for `version` and `distribution` " +
                          "will be interpolated into this string wherever `${version}` and `${distribution}` appear respectively. " +
                          "Defaults to https://services.gradle.org/distributions/gradle-${version}-${distribution}.zip.",
            required = false)
    @Nullable
    final String wrapperUri;

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (version != null) {
            validated = validated.and(Semver.validate(version, null));
        }
        return validated;
    }

    @NonFinal
    transient GradleWrapper gradleWrapper;

    private GradleWrapper getGradleWrapper(ExecutionContext ctx) {
        if (gradleWrapper == null) {
            gradleWrapper = GradleWrapper.create(distribution, version, null, ctx);
        }
        return gradleWrapper;
    }

    public static class GradleWrapperState {
        boolean needsWrapperUpdate = false;
        BuildTool updatedMarker;
        boolean addGradleWrapperProperties = true;
        boolean addGradleWrapperJar = true;
        boolean addGradleShellScript = true;
        boolean addGradleBatchScript = true;
    }

    @Override
    public GradleWrapperState getInitialValue(ExecutionContext ctx) {
        return new GradleWrapperState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(GradleWrapperState acc) {
        return Preconditions.or(
                new PropertiesVisitor<ExecutionContext>() {
                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                        if (!super.isAcceptable(sourceFile, ctx)) {
                            return false;
                        }

                        if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                            acc.addGradleWrapperProperties = false;
                        } else if (!PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH)) {
                            return false;
                        }

                        Optional<BuildTool> maybeBuildTool = sourceFile.getMarkers().findFirst(BuildTool.class);
                        if (!maybeBuildTool.isPresent()) {
                            return false;
                        }
                        BuildTool buildTool = maybeBuildTool.get();
                        if (buildTool.getType() != BuildTool.Type.Gradle) {
                            return false;
                        }

                        GradleWrapper gradleWrapper = getGradleWrapper(ctx);

                        VersionComparator versionComparator = requireNonNull(Semver.validate(isBlank(version) ? "latest.release" : version, null).getValue());
                        int compare = versionComparator.compare(null, buildTool.getVersion(), gradleWrapper.getVersion());
                        // maybe we want to update the distribution type or url
                        if (compare < 0) {
                            acc.needsWrapperUpdate = true;
                            acc.updatedMarker = buildTool.withVersion(gradleWrapper.getVersion());
                            return true;
                        } else return compare == 0;
                    }

                    @Override
                    public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                        if (!"distributionUrl".equals(entry.getKey())) {
                            return entry;
                        }

                        GradleWrapper gradleWrapper = getGradleWrapper(ctx);

                        // Typical example: https://services.gradle.org/distributions/gradle-7.4-all.zip
                        String currentDistributionUrl = entry.getValue().getText();
                        if (!gradleWrapper.getPropertiesFormattedUrl().equals(currentDistributionUrl)) {
                            acc.needsWrapperUpdate = true;
                        }
                        return entry;
                    }
                },
                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                        if (!super.isAcceptable(sourceFile, ctx)) {
                            return false;
                        }

                        if ((sourceFile instanceof Quark || sourceFile instanceof Remote) &&
                            equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                            acc.addGradleWrapperJar = false;
                            return true;
                        }

                        if (sourceFile instanceof PlainText) {
                            if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                                acc.addGradleBatchScript = false;
                                return true;
                            } else if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                                acc.addGradleShellScript = false;
                                return true;
                            }
                        }

                        return false;
                    }
                }
        );
    }

    @Override
    public Collection<SourceFile> generate(GradleWrapperState acc, ExecutionContext ctx) {
        if (Boolean.FALSE.equals(addIfMissing)) {
            return Collections.emptyList();
        }

        if (!(acc.addGradleWrapperJar || acc.addGradleWrapperProperties || acc.addGradleBatchScript || acc.addGradleShellScript)) {
            return Collections.emptyList();
        }

        List<SourceFile> gradleWrapperFiles = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();

        GradleWrapper gradleWrapper = getGradleWrapper(ctx);

        if (acc.addGradleWrapperProperties) {
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
        if (acc.addGradleShellScript) {
            String gradlewText = unixScript(gradleWrapper, ctx);
            PlainText gradlew = PlainText.builder()
                    .text(gradlewText)
                    .sourcePath(WRAPPER_SCRIPT_LOCATION)
                    .fileAttributes(wrapperScriptAttributes)
                    .build();
            gradleWrapperFiles.add(gradlew);
        }

        if (acc.addGradleBatchScript) {
            String gradlewBatText = batchScript(gradleWrapper, ctx);
            PlainText gradlewBat = PlainText.builder()
                    .text(gradlewBatText)
                    .sourcePath(WRAPPER_BATCH_LOCATION)
                    .fileAttributes(wrapperScriptAttributes)
                    .build();
            gradleWrapperFiles.add(gradlewBat);
        }

        if (acc.addGradleWrapperJar) {
            gradleWrapperFiles.add(gradleWrapper.wrapperJar());
        }

        return gradleWrapperFiles;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(GradleWrapperState acc) {
        if (!acc.needsWrapperUpdate) {
            return TreeVisitor.noop();
        }

        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }

                SourceFile sourceFile = (SourceFile) tree;
                if (acc.updatedMarker != null) {
                    Optional<BuildTool> maybeCurrentMarker = sourceFile.getMarkers().findFirst(BuildTool.class);
                    if (maybeCurrentMarker.isPresent()) {
                        BuildTool currentMarker = maybeCurrentMarker.get();
                        if (currentMarker.getType() != BuildTool.Type.Gradle) {
                            return sourceFile;
                        }
                        VersionComparator versionComparator = requireNonNull(Semver.validate(isBlank(version) ? "latest.release" : version, null).getValue());
                        int compare = versionComparator.compare(null, currentMarker.getVersion(), acc.updatedMarker.getVersion());
                        if (compare < 0) {
                            sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().setByType(acc.updatedMarker));
                        } else {
                            return sourceFile;
                        }
                    }
                }

                if (sourceFile instanceof PlainText && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_SCRIPT_LOCATION_RELATIVE_PATH)) {
                    String gradlewText = unixScript(gradleWrapper, ctx);
                    PlainText gradlew = (PlainText) setExecutable(sourceFile);
                    if (!gradlewText.equals(gradlew.getText())) {
                        gradlew = gradlew.withText(gradlewText);
                    }
                    return gradlew;
                }
                if (sourceFile instanceof PlainText && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_BATCH_LOCATION_RELATIVE_PATH)) {
                    String gradlewBatText = batchScript(gradleWrapper, ctx);
                    PlainText gradlewBat = (PlainText) setExecutable(sourceFile);
                    if (!gradlewBatText.equals(gradlewBat.getText())) {
                        gradlewBat = gradlewBat.withText(gradlewBatText);
                    }
                    return gradlewBat;
                }
                if (sourceFile instanceof Properties.File && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH)) {
                    return new WrapperPropertiesVisitor(gradleWrapper).visitNonNull(sourceFile, ctx);
                }
                if ((sourceFile instanceof Quark || sourceFile instanceof Remote) && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_JAR_LOCATION_RELATIVE_PATH)) {
                    return gradleWrapper.wrapperJar(sourceFile);
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
        } else if (!attributes.isExecutable()) {
            return sourceFile.withFileAttributes(attributes.withExecutable(true));
        }
        return sourceFile;
    }

    private String unixScript(GradleWrapper gradleWrapper, ExecutionContext ctx) {
        Map<String, String> binding = new HashMap<>();
        String defaultJvmOpts = defaultJvmOpts(gradleWrapper);
        binding.put("defaultJvmOpts", StringUtils.isNotEmpty(defaultJvmOpts) ? "'" + defaultJvmOpts + "'" : "");
        binding.put("classpath", "$APP_HOME/gradle/wrapper/gradle-wrapper.jar");

        String gradlewTemplate = StringUtils.readFully(gradleWrapper.gradlew().getInputStream(ctx));
        return renderTemplate(gradlewTemplate, binding, "\n");
    }

    private String batchScript(GradleWrapper gradleWrapper, ExecutionContext ctx) {
        Map<String, String> binding = new HashMap<>();
        binding.put("defaultJvmOpts", defaultJvmOpts(gradleWrapper));
        binding.put("classpath", "%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar");

        String gradlewBatTemplate = StringUtils.readFully(gradleWrapper.gradlewBat().getInputStream(ctx));
        return renderTemplate(gradlewBatTemplate, binding, "\r\n");
    }

    private String defaultJvmOpts(GradleWrapper gradleWrapper) {
        VersionComparator gradle53VersionComparator = requireNonNull(Semver.validate("[5.3,)", null).getValue());
        VersionComparator gradle50VersionComparator = requireNonNull(Semver.validate("[5.0,)", null).getValue());

        if (gradle53VersionComparator.isValid(null, gradleWrapper.getVersion())) {
            return "\"-Xmx64m\" \"-Xms64m\"";
        } else if (gradle50VersionComparator.isValid(null, gradleWrapper.getVersion())) {
            return "\"-Xmx64m\"";
        }
        return "";
    }

    private String renderTemplate(String source, Map<String, String> parameters, String lineSeparator) {
        Map<String, String> binding = new HashMap<>(parameters);
        binding.put("applicationName", "Gradle");
        binding.put("optsEnvironmentVar", "GRADLE_OPTS");
        binding.put("exitEnvironmentVar", "GRADLE_EXIT_CONSOLE");
        binding.put("mainClassName", "org.gradle.wrapper.GradleWrapperMain");
        binding.put("appNameSystemProperty", "org.gradle.appname");
        binding.put("appHomeRelativePath", "");
        binding.put("modulePath", "");

        String script = source;
        for (Map.Entry<String, String> variable : binding.entrySet()) {
            script = script.replace("${" + variable.getKey() + "}", variable.getValue())
                    .replace("$" + variable.getKey(), variable.getValue());
        }

        script = script.replaceAll("(?sm)<% /\\*.*?\\*/ %>", "");
        script = script.replaceAll("(?sm)<% if \\( mainClassName\\.startsWith\\('--module '\\) \\) \\{.*?} %>", "");
        script = script.replaceAll("(?sm)<% if \\( appNameSystemProperty \\) \\{.*?%>(.*?)<% } %>", "$1");
        script = script.replace("\\$", "$");
        script = script.replaceAll("DIRNAME=\\.\\\\[\r\n]", "DIRNAME=.");
        script = script.replace("\\\\", "\\");
        script = script.replaceAll("\r\n|\r|\n", lineSeparator);

        return script;
    }

    private class WrapperPropertiesVisitor extends PropertiesVisitor<ExecutionContext> {

        private static final String DISTRIBUTION_SHA_256_SUM_KEY = "distributionSha256Sum";
        private final GradleWrapper gradleWrapper;

        public WrapperPropertiesVisitor(GradleWrapper gradleWrapper) {
            this.gradleWrapper = gradleWrapper;
        }

        @Override
        public Properties visitFile(Properties.File file, ExecutionContext ctx) {
            Properties p = super.visitFile(file, ctx);
            Set<Properties.Entry> checksumKey = FindProperties.find(p, DISTRIBUTION_SHA_256_SUM_KEY, false);
            if (checksumKey.isEmpty()) {
                Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, gradleWrapper.getDistributionChecksum().getHexValue());
                Properties.Entry entry = new Properties.Entry(Tree.randomId(), "\n", Markers.EMPTY, DISTRIBUTION_SHA_256_SUM_KEY, "", Properties.Entry.Delimiter.EQUALS, propertyValue);
                List<Properties.Content> contentList = ListUtils.concat(((Properties.File) p).getContent(), entry);
                p = ((Properties.File) p).withContent(contentList);
            }
            return p;
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
            if ("distributionUrl".equals(entry.getKey())) {
                Properties.Value value = entry.getValue();
                String currentUrl = value.getText();
                // Prefer wrapperUri specified directly in the recipe over other options
                // If that isn't set, prefer the existing artifact repository URL over changing to services.gradle.org
                if(wrapperUri != null) {
                    String effectiveWrapperUri = formatUriForPropertiesFile(wrapperUri
                            .replace("${version}", gradleWrapper.getVersion())
                            .replace("${distribution}", distribution == null ? "bin" : distribution));
                    return entry.withValue(value.withText(effectiveWrapperUri));
                } else if(currentUrl.startsWith("https\\://services.gradle.org/distributions/")) {
                    return entry.withValue(value.withText(gradleWrapper.getPropertiesFormattedUrl()));
                } else {
                    String gradleServicesDistributionUrl = gradleWrapper.getDistributionUrl();
                    String newDistributionFile = gradleServicesDistributionUrl.substring(gradleServicesDistributionUrl.lastIndexOf('/') + 1);
                    String repositoryUrlPrefix = currentUrl.substring(0, currentUrl.lastIndexOf('/'));
                    return entry.withValue(value.withText(repositoryUrlPrefix + "/" + newDistributionFile));
                }
            }
            if (DISTRIBUTION_SHA_256_SUM_KEY.equals(entry.getKey())) {
                return entry.withValue(entry.getValue().withText(gradleWrapper.getDistributionChecksum().getHexValue()));
            }
            return entry;
        }
    }
}
