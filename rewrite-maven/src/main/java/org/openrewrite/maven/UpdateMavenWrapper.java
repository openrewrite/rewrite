/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.utilities.MavenWrapper;
import org.openrewrite.properties.PropertiesIsoVisitor;
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
import static org.openrewrite.internal.StringUtils.isBlank;
import static org.openrewrite.maven.utilities.MavenWrapper.*;

/**
 * This recipe expects for the specified repository to be a Maven layout with `maven-metadata.xml` files containing all
 * the following REQUIRED publications:
 * <br/>
 * org.apache.maven.wrapper:maven-wrapper:{wrapperVersion}
 * org.apache.maven.wrapper:maven-wrapper-distribution:{wrapperVersion}
 * org.apache.maven:apache-maven:{distributionVersion}
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public class UpdateMavenWrapper extends ScanningRecipe<UpdateMavenWrapper.MavenWrapperState> {
    private static final String DISTRIBUTION_URL_KEY = "distributionUrl";
    private static final String DISTRIBUTION_SHA_256_SUM_KEY = "distributionSha256Sum";
    private static final String WRAPPER_URL_KEY = "wrapperUrl";
    private static final String WRAPPER_SHA_256_SUM_KEY = "wrapperSha256Sum";

    @Getter
    @Option(displayName = "New wrapper version",
            description = "An exact version number or node-style semver selector used to select the wrapper version number.",
            example = "3.x",
            required = false)
    @Nullable
    final String wrapperVersion;

    @Getter
    @Option(displayName = "Wrapper Distribution type",
            description = "The distribution of the Maven wrapper to use.\n\n" +
                          "* \"bin\" uses a `maven-wrapper.jar` compiled binary.\n" +
                          "* \"only-script\" uses a lite version of `mvnw`/`mvnw.cmd` using wget/curl or powershell. (required wrapper 3.2.0 or newer)\n" +
                          "* \"script\" downloads `maven-wrapper.jar` or `MavenWrapperDownloader.java` to then download a full distribution.\n" +
                          "* \"source\" uses `MavenWrapperDownloader.java` source file.\n\n" +
                          "Defaults to \"bin\".",
            valid = {"bin", "only-script", "script", "source"},
            required = false)
    @Nullable
    final String wrapperDistribution;

    @Getter
    @Option(displayName = "New distribution version",
            description = "An exact version number or node-style semver selector used to select the Maven version number.",
            example = "3.x",
            required = false)
    @Nullable
    final String distributionVersion;

    @Getter
    @Option(displayName = "Repository URL",
            description = "The URL of the repository to download the Maven wrapper and distribution from. Supports repositories " +
                          "with a Maven layout. Defaults to `https://repo.maven.apache.org/maven2`.",
            example = "https://repo.maven.apache.org/maven2",
            required = false)
    @Nullable
    final String repositoryUrl;

    @Getter
    @Option(displayName = "Add if missing",
            description = "Add a Maven wrapper, if it's missing. Defaults to `true`.",
            required = false)
    @Nullable
    final Boolean addIfMissing;

    @Getter
    @Option(displayName = "Enforce checksum verification for maven-wrapper.jar",
            description = "Enforce checksum verification for the maven-wrapper.jar. Enabling this feature may sporadically " +
                          "result in build failures, such as [MWRAPPER-103](https://issues.apache.org/jira/browse/MWRAPPER-103). Defaults to `false`.",
            required = false)
    @Nullable
    final Boolean enforceWrapperChecksumVerification;

    @Override
    public String getDisplayName() {
        return "Update Maven wrapper";
    }

    @Override
    public String getDescription() {
        return "Update the version of Maven used in an existing Maven wrapper.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (wrapperVersion != null) {
            validated = validated.and(Semver.validate(wrapperVersion, null));
        }
        if (distributionVersion != null) {
            validated = validated.and(Semver.validate(distributionVersion, null));
        }
        return validated;
    }

    @NonFinal
    @Nullable
    transient MavenWrapper mavenWrapper;

    private MavenWrapper getMavenWrapper(ExecutionContext ctx) {
        if (mavenWrapper == null) {
            mavenWrapper = MavenWrapper.create(wrapperVersion, wrapperDistribution, distributionVersion, repositoryUrl, ctx);
        }
        return mavenWrapper;
    }

    static class MavenWrapperState {
        boolean needsWrapperUpdate = false;
        @Nullable BuildTool updatedMarker;
        boolean addMavenWrapperProperties = true;
        boolean addMavenWrapperDownloader = true;
        boolean addMavenWrapperJar = true;
        boolean addMavenShellScript = true;
        boolean addMavenBatchScript = true;
    }

    @Override
    public MavenWrapperState getInitialValue(ExecutionContext ctx) {
        return new MavenWrapperState();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MavenWrapperState acc) {
        return Preconditions.or(
                new PropertiesVisitor<ExecutionContext>() {
                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                        if (!super.isAcceptable(sourceFile, ctx)) {
                            return false;
                        }

                        if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_PROPERTIES_LOCATION)) {
                            acc.addMavenWrapperProperties = false;
                        } else if (!PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH)) {
                            return false;
                        }

                        Optional<BuildTool> maybeBuildTool = sourceFile.getMarkers().findFirst(BuildTool.class);
                        if (!maybeBuildTool.isPresent()) {
                            return false;
                        }
                        BuildTool buildTool = maybeBuildTool.get();
                        if (buildTool.getType() != BuildTool.Type.Maven) {
                            return false;
                        }

                        MavenWrapper mavenWrapper = getMavenWrapper(ctx);

                        VersionComparator versionComparator = requireNonNull(Semver.validate(isBlank(distributionVersion) ? "latest.release" : distributionVersion, null).getValue());
                        int compare = versionComparator.compare(null, buildTool.getVersion(), mavenWrapper.getDistributionVersion());
                        // maybe we want to update the distribution url
                        if (compare < 0) {
                            acc.needsWrapperUpdate = true;
                            acc.updatedMarker = buildTool.withVersion(mavenWrapper.getDistributionVersion());
                            return true;
                        } else return compare == 0;
                    }

                    @Override
                    public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                        Properties p = super.visitFile(file, ctx);
                        if (FindProperties.find(p, DISTRIBUTION_SHA_256_SUM_KEY, null).isEmpty() ||
                            (FindProperties.find(p, WRAPPER_SHA_256_SUM_KEY, null).isEmpty() && Boolean.TRUE.equals(enforceWrapperChecksumVerification))) {
                            acc.needsWrapperUpdate = true;
                        }
                        return p;
                    }

                    @Override
                    public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                        MavenWrapper mavenWrapper = getMavenWrapper(ctx);
                        if ("distributionUrl".equals(entry.getKey())) {
                            // Typical example: https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.0/apache-maven-3.8.0-bin.zip
                            String currentDistributionUrl = entry.getValue().getText();
                            if (!mavenWrapper.getDistributionUrl().equals(currentDistributionUrl)) {
                                acc.needsWrapperUpdate = true;
                            }
                        } else if ("wrapperUrl".equals(entry.getKey())) {
                            // Typical example: https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.0/maven-wrapper-3.1.0.jar
                            String currentWrapperUrl = entry.getValue().getText();
                            if (!mavenWrapper.getWrapperUrl().equals(currentWrapperUrl)) {
                                acc.needsWrapperUpdate = true;
                            }
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

                        MavenWrapper mavenWrapper = getMavenWrapper(ctx);

                        if (sourceFile instanceof Quark || sourceFile instanceof Remote) {
                            if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_JAR_LOCATION)) {
                                acc.addMavenWrapperJar = false;
                                if (mavenWrapper.getWrapperDistributionType() != DistributionType.Bin) {
                                    acc.needsWrapperUpdate = true;
                                }
                                return true;
                            } else if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_DOWNLOADER_LOCATION)) {
                                acc.addMavenWrapperDownloader = false;
                                if (mavenWrapper.getWrapperDistributionType() != DistributionType.Source) {
                                    acc.needsWrapperUpdate = true;
                                }
                                return true;
                            }
                        }

                        if (sourceFile instanceof PlainText) {
                            if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_BATCH_LOCATION)) {
                                acc.addMavenBatchScript = false;
                                return true;
                            } else if (equalIgnoringSeparators(sourceFile.getSourcePath(), WRAPPER_SCRIPT_LOCATION)) {
                                acc.addMavenShellScript = false;
                                return true;
                            }
                        }

                        return false;
                    }
                }
        );
    }

    @Override
    public Collection<SourceFile> generate(MavenWrapperState acc, ExecutionContext ctx) {
        if (Boolean.FALSE.equals(addIfMissing)) {
            return Collections.emptyList();
        }

        MavenWrapper mavenWrapper = getMavenWrapper(ctx);
        if (mavenWrapper.getWrapperDistributionType() == DistributionType.Bin) {
            if (!(acc.addMavenWrapperJar || acc.addMavenWrapperProperties || acc.addMavenBatchScript || acc.addMavenShellScript)) {
                return Collections.emptyList();
            }
        } else if (mavenWrapper.getWrapperDistributionType() == DistributionType.OnlyScript) {
            if (!(acc.addMavenWrapperProperties || acc.addMavenBatchScript || acc.addMavenShellScript)) {
                return Collections.emptyList();
            }
        } else {
            if (!(acc.addMavenWrapperDownloader || acc.addMavenWrapperProperties || acc.addMavenBatchScript || acc.addMavenShellScript)) {
                return Collections.emptyList();
            }
        }

        List<SourceFile> mavenWrapperFiles = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();

        if (acc.addMavenWrapperProperties) {
            @Language("properties")
            String mavenWrapperPropertiesText = ASF_LICENSE_HEADER +
                                                DISTRIBUTION_URL_KEY + "=" + mavenWrapper.getDistributionUrl() + "\n" +
                                                DISTRIBUTION_SHA_256_SUM_KEY + "=" + mavenWrapper.getDistributionChecksum().getHexValue();
            if (mavenWrapper.getWrapperDistributionType() != DistributionType.OnlyScript) {
                mavenWrapperPropertiesText += "\n" +
                                              WRAPPER_URL_KEY + "=" + mavenWrapper.getWrapperUrl();
                if (Boolean.TRUE.equals(enforceWrapperChecksumVerification)) {
                    mavenWrapperPropertiesText += "\n" +
                            WRAPPER_SHA_256_SUM_KEY + "=" + mavenWrapper.getWrapperChecksum().getHexValue();
                }
            }
            //noinspection UnusedProperty
            Properties.File mavenWrapperProperties = new PropertiesParser().parse(mavenWrapperPropertiesText)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as properties"))
                    .withSourcePath(WRAPPER_PROPERTIES_LOCATION);
            mavenWrapperFiles.add(mavenWrapperProperties);
        }

        FileAttributes wrapperScriptAttributes = new FileAttributes(now, now, now, true, true, true, 1L);
        if (acc.addMavenShellScript) {
            String mvnwText = unixScript(mavenWrapper, ctx);
            PlainText mvnw = PlainText.builder()
                    .text(mvnwText)
                    .sourcePath(WRAPPER_SCRIPT_LOCATION)
                    .fileAttributes(wrapperScriptAttributes)
                    .build();
            mavenWrapperFiles.add(mvnw);
        }

        if (acc.addMavenBatchScript) {
            String mvnwCmdText = batchScript(mavenWrapper, ctx);
            PlainText mvnwCmd = PlainText.builder()
                    .text(mvnwCmdText)
                    .sourcePath(WRAPPER_BATCH_LOCATION)
                    .fileAttributes(wrapperScriptAttributes)
                    .build();
            mavenWrapperFiles.add(mvnwCmd);
        }

        if (mavenWrapper.getWrapperDistributionType() == DistributionType.Bin && acc.addMavenWrapperJar) {
            mavenWrapperFiles.add(mavenWrapper.wrapperJar());
        } else if (mavenWrapper.getWrapperDistributionType() == DistributionType.Source && acc.addMavenWrapperDownloader) {
            mavenWrapperFiles.add(mavenWrapper.wrapperDownloader());
        }

        return mavenWrapperFiles;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MavenWrapperState acc) {
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
                        if (currentMarker.getType() != BuildTool.Type.Maven) {
                            return sourceFile;
                        }
                        VersionComparator versionComparator = requireNonNull(Semver.validate(isBlank(distributionVersion) ? "latest.release" : distributionVersion, null).getValue());
                        int compare = versionComparator.compare(null, currentMarker.getVersion(), acc.updatedMarker.getVersion());
                        if (compare < 0) {
                            sourceFile = sourceFile.withMarkers(sourceFile.getMarkers().setByType(acc.updatedMarker));
                        } else {
                            return sourceFile;
                        }
                    }
                }

                MavenWrapper mavenWrapper = getMavenWrapper(ctx);

                if (sourceFile instanceof PlainText && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_SCRIPT_LOCATION_RELATIVE_PATH)) {
                    String mvnwText = unixScript(mavenWrapper, ctx);
                    PlainText mvnw = (PlainText) setExecutable(sourceFile);
                    if (!mvnwText.equals(mvnw.getText())) {
                        mvnw = mvnw.withText(mvnwText);
                    }
                    return mvnw;
                }
                if (sourceFile instanceof PlainText && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_BATCH_LOCATION_RELATIVE_PATH)) {
                    String mvnwCmdText = batchScript(mavenWrapper, ctx);
                    PlainText mvnwCmd = (PlainText) setExecutable(sourceFile);
                    if (!mvnwCmdText.equals(mvnwCmd.getText())) {
                        mvnwCmd = mvnwCmd.withText(mvnwCmdText);
                    }
                    return mvnwCmd;
                }

                if (sourceFile instanceof Properties.File && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_PROPERTIES_LOCATION_RELATIVE_PATH)) {
                    return new WrapperPropertiesVisitor(mavenWrapper).visitNonNull(sourceFile, ctx);
                }
                if (mavenWrapper.getWrapperDistributionType() == DistributionType.Bin) {
                    if ((sourceFile instanceof Quark || sourceFile instanceof Remote) && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_JAR_LOCATION_RELATIVE_PATH)) {
                        return mavenWrapper.wrapperJar(sourceFile);
                    }

                    if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_DOWNLOADER_LOCATION_RELATIVE_PATH)) {
                        return null;
                    }
                } else if (mavenWrapper.getWrapperDistributionType() == DistributionType.Source) {
                    if ((sourceFile instanceof Quark || sourceFile instanceof Remote) && PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_DOWNLOADER_LOCATION_RELATIVE_PATH)) {
                        return mavenWrapper.wrapperDownloader(sourceFile);
                    }

                    if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_JAR_LOCATION_RELATIVE_PATH)) {
                        return null;
                    }
                } else if (PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_JAR_LOCATION_RELATIVE_PATH) ||
                           PathUtils.matchesGlob(sourceFile.getSourcePath(), "**/" + WRAPPER_DOWNLOADER_LOCATION_RELATIVE_PATH)) {
                    return null;
                }
                return sourceFile;
            }
        };
    }

    private static <T extends SourceFile> T setExecutable(T sourceFile) {
        FileAttributes attributes = sourceFile.getFileAttributes();
        if (attributes == null) {
            ZonedDateTime now = ZonedDateTime.now();
            return sourceFile.withFileAttributes(new FileAttributes(now, now, now, true, true, true, 1L));
        } else if (!attributes.isExecutable()) {
            return sourceFile.withFileAttributes(attributes.withExecutable(true));
        }
        return sourceFile;
    }

    private String unixScript(MavenWrapper mavenWrapper, ExecutionContext ctx) {
        return StringUtils.readFully(mavenWrapper.mvnw().getInputStream(ctx));
    }

    private String batchScript(MavenWrapper mavenWrapper, ExecutionContext ctx) {
        return StringUtils.readFully(mavenWrapper.mvnwCmd().getInputStream(ctx));
    }

    @AllArgsConstructor
    private class WrapperPropertiesVisitor extends PropertiesIsoVisitor<ExecutionContext> {
        MavenWrapper mavenWrapper;

        @Override
        public Properties.File visitFile(Properties.File file, ExecutionContext ctx) {
            Properties.File p = super.visitFile(file, ctx);
            Checksum mavenDistributionChecksum = mavenWrapper.getDistributionChecksum();
            if (FindProperties.find(p, DISTRIBUTION_SHA_256_SUM_KEY, null).isEmpty() && mavenDistributionChecksum != null) {
                Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, mavenDistributionChecksum.getHexValue());
                Properties.Entry entry = new Properties.Entry(Tree.randomId(), "\n", Markers.EMPTY, DISTRIBUTION_SHA_256_SUM_KEY, "", Properties.Entry.Delimiter.EQUALS, propertyValue);
                p = p.withContent(ListUtils.concat(p.getContent(), entry));
            }
            if (mavenWrapper.getWrapperDistributionType() != DistributionType.OnlyScript && Boolean.TRUE.equals(enforceWrapperChecksumVerification)) {
                Checksum wrapperJarChecksum = mavenWrapper.getWrapperChecksum();
                if (FindProperties.find(p, WRAPPER_SHA_256_SUM_KEY, null).isEmpty() && wrapperJarChecksum != null) {
                    Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, wrapperJarChecksum.getHexValue());
                    Properties.Entry entry = new Properties.Entry(Tree.randomId(), "\n", Markers.EMPTY, WRAPPER_SHA_256_SUM_KEY, "", Properties.Entry.Delimiter.EQUALS, propertyValue);
                    p = p.withContent(ListUtils.concat(p.getContent(), entry));
                }
            }
            return p;
        }

        @Override
        public Properties.Entry visitEntry(Properties.Entry entry, ExecutionContext ctx) {
            if (DISTRIBUTION_URL_KEY.equals(entry.getKey())) {
                Properties.Value value = entry.getValue();
                if (!mavenWrapper.getDistributionUrl().equals(value.getText())) {
                    return entry.withValue(value.withText(mavenWrapper.getDistributionUrl()));
                }
            } else if (DISTRIBUTION_SHA_256_SUM_KEY.equals(entry.getKey())) {
                Properties.Value value = entry.getValue();
                Checksum mavenDistributionChecksum = mavenWrapper.getDistributionChecksum();
                if (mavenDistributionChecksum != null && !mavenDistributionChecksum.getHexValue().equals(value.getText())) {
                    return entry.withValue(value.withText(mavenDistributionChecksum.getHexValue()));
                }
            } else if (WRAPPER_URL_KEY.equals(entry.getKey())) {
                if (mavenWrapper.getWrapperDistributionType() != DistributionType.OnlyScript) {
                    Properties.Value value = entry.getValue();
                    if (!mavenWrapper.getWrapperUrl().equals(value.getText())) {
                        return entry.withValue(value.withText(mavenWrapper.getWrapperUrl()));
                    }
                } else {
                    //noinspection ConstantConditions
                    return null;
                }
            } else if (WRAPPER_SHA_256_SUM_KEY.equals(entry.getKey())) {
                if (mavenWrapper.getWrapperDistributionType() != DistributionType.OnlyScript
                        && Boolean.TRUE.equals(enforceWrapperChecksumVerification)) {
                    Properties.Value value = entry.getValue();
                    Checksum wrapperJarChecksum = mavenWrapper.getWrapperChecksum();
                    if (wrapperJarChecksum != null && !wrapperJarChecksum.getHexValue().equals(value.getText())) {
                        return entry.withValue(value.withText(wrapperJarChecksum.getHexValue()));
                    }
                } else {
                    //noinspection ConstantConditions
                    return null;
                }
            }
            return entry;
        }
    }
}
