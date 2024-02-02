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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.PathUtils;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


@Value
@EqualsAndHashCode(callSuper = false)
public class AddDevelocityMavenExtension extends ScanningRecipe<AddDevelocityMavenExtension.Accumulator> {
    private static final String GRADLE_ENTERPRISE_MAVEN_EXTENSION_ARTIFACT_ID = "gradle-enterprise-maven-extension";
    private static final String EXTENSIONS_XML_PATH = ".mvn/extensions.xml";
    private static final String GRADLE_ENTERPRISE_XML_PATH = ".mvn/gradle-enterprise.xml";

    @Language("xml")
    private static final String EXTENSIONS_XML_FORMAT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                        "<extensions>\n" +
                                                        "</extensions>";

    @Language("xml")
    private static final String ENTERPRISE_TAG_FORMAT = "<extension>\n" +
                                                        "  <groupId>com.gradle</groupId>\n" +
                                                        "  <artifactId>gradle-enterprise-maven-extension</artifactId>\n" +
                                                        "  <version>%s</version>\n" +
                                                        "</extension>";

    @Option(displayName = "Extension version",
            description = "A maven-compatible version number to select the gradle-enterprise-maven-extension version.",
            required = false,
            example = "1.17.4")
    @Nullable
    String version;

    @Option(displayName = "Server URL",
            description = "The URL of the Develocity server.",
            example = "https://scans.gradle.com/")
    String server;

    @Option(displayName = "Allow untrusted server",
            description = "When set to `true` the extension will be configured to allow unencrypted http connections with the server. " +
                          "If set to `false` or omitted, the extension will refuse to communicate without transport layer security enabled.",
            required = false,
            example = "true")
    @Nullable
    Boolean allowUntrustedServer;

    @Option(displayName = "Capture goal input files",
            description = "When set to `true` the extension will capture additional information about the inputs to Maven goals. " +
                          "This increases the size of build scans, but is useful for diagnosing issues with goal caching. ",
            required = false,
            example = "true")
    @Nullable
    Boolean captureGoalInputFiles;

    @Option(displayName = "Upload in background",
            description = "When set to `false` the extension will not upload build scan in the background. " +
                          "By default, build scans are uploaded in the background after the build has finished to avoid blocking the build process.",
            required = false,
            example = "false")
    @Nullable
    Boolean uploadInBackground;

    @Option(displayName = "Publish Criteria",
            description = "When set to `Always` the extension will publish build scans of every single build. " +
                          "This is the default behavior when omitted." +
                          "When set to `Failure` the extension will only publish build scans when the build fails. " +
                          "When set to `Demand` the extension will only publish build scans when explicitly requested.",
            required = false,
            valid = {"Always", "Failure", "Demand"},
            example = "Always")
    @Nullable
    PublishCriteria publishCriteria;

    public enum PublishCriteria {
        Always("ALWAYS"),
        Failure("ON_FAILURE"),
        Demand("ON_DEMAND");

        private final String xmlName;

        PublishCriteria(String xmlName) {
            this.xmlName = xmlName;
        }
    }

    @Override
    public String getDisplayName() {
        return "Add the Develocity Maven extension";
    }

    @Override
    public String getDescription() {
        return "To integrate the Develocity Maven extension into Maven projects, ensure that the " +
               "`gradle-enterprise-maven-extension` is added to the `.mvn/extensions.xml` file if not already present. " +
               "Additionally, configure the extension by adding the `.mvn/gradle-enterprise.xml` configuration file.";
    }

    @Data
    static class Accumulator {
        boolean mavenProject;
        boolean useCRLFNewLines;
        Path matchingExtensionsXmlFile;
        Path matchingGradleEnterpriseXmlFile;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }

                SourceFile sourceFile = (SourceFile) tree;
                String sourcePath = PathUtils.separatorsToUnix(sourceFile.getSourcePath().toString());
                switch (sourcePath) {
                    case "pom.xml":
                        acc.setMavenProject(true);
                        acc.setUseCRLFNewLines(sourceFile.getStyle(GeneralFormatStyle.class, new GeneralFormatStyle(false))
                                .isUseCRLFNewLines());
                        break;
                    case EXTENSIONS_XML_PATH:
                        if (!(sourceFile instanceof Xml.Document)) {
                            throw new RuntimeException("The extensions.xml is not xml document type");
                        }
                        acc.setMatchingExtensionsXmlFile(sourceFile.getSourcePath());
                        break;
                    case GRADLE_ENTERPRISE_XML_PATH:
                        acc.setMatchingGradleEnterpriseXmlFile(sourceFile.getSourcePath());
                        break;
                    default:
                        break;
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
        // This recipe makes change for maven project only, or if the file `.mvn/gradle-enterprise.xml` already exists, do nothing
        if (!acc.isMavenProject() || acc.getMatchingGradleEnterpriseXmlFile() != null) {
            return Collections.emptyList();
        }

        List<SourceFile> sources = new ArrayList<>();
        sources.add(createNewXml(GRADLE_ENTERPRISE_XML_PATH, gradleEnterpriseConfiguration(acc.isUseCRLFNewLines())));

        if (acc.getMatchingExtensionsXmlFile() == null) {
            Xml.Document extensionsXml = createNewXml(EXTENSIONS_XML_PATH, EXTENSIONS_XML_FORMAT);
            extensionsXml = addEnterpriseExtension(extensionsXml, ctx);
            sources.add(extensionsXml);
        }

        return sources;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (!acc.isMavenProject() || acc.getMatchingExtensionsXmlFile() == null) {
            return TreeVisitor.noop();
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }

                SourceFile sourceFile = (SourceFile) tree;
                if (sourceFile.getSourcePath().equals(acc.getMatchingExtensionsXmlFile())) {
                    Xml.Document extensionsXml = (Xml.Document) sourceFile;

                    // find `gradle-enterprise-maven-extension` extension, do nothing if it already exists,
                    boolean hasEnterpriseExtension = findExistingEnterpriseExtension(extensionsXml);
                    if (hasEnterpriseExtension) {
                        return sourceFile;
                    }

                    return addEnterpriseExtension(extensionsXml, ctx);
                }
                return tree;
            }
        };
    }

    @JacksonXmlRootElement(localName = "gradleEnterprise")
    @Value
    private static class GradleEnterpriseConfiguration {
        ServerConfiguration server;
        @Nullable
        BuildScanConfiguration buildScan;
    }

    @Value
    private static class ServerConfiguration {
        String url;
        @Nullable
        Boolean allowUntrusted;
    }

    @Value
    private static class BuildScanConfiguration {
        @Nullable
        Boolean backgroundBuildScanUpload;
        @Nullable
        String publish;
        @Nullable
        Capture capture;
    }

    @Value
    private static class Capture {
        Boolean goalInputFiles;
    }

    private String gradleEnterpriseConfiguration(boolean useCRLFNewLines) {
        BuildScanConfiguration buildScanConfiguration = buildScanConfiguration();
        ServerConfiguration serverConfiguration = new ServerConfiguration(server, allowUntrustedServer);
        try {
            ObjectMapper objectMapper = MavenXmlMapper.writeMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
            PrettyPrinter pp = new DefaultXmlPrettyPrinter().withCustomNewLine(useCRLFNewLines ? "\r\n" : "\n");
            return objectMapper.writer(pp)
                    .writeValueAsString(new GradleEnterpriseConfiguration(serverConfiguration, buildScanConfiguration));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private BuildScanConfiguration buildScanConfiguration() {
        if (uploadInBackground != null || publishCriteria != null || captureGoalInputFiles != null) {
            return new BuildScanConfiguration(uploadInBackground,
                    publishCriteria != null ? publishCriteria.xmlName : null,
                    captureGoalInputFiles != null ? new Capture(captureGoalInputFiles) : null);
        }
        return null;
    }

    private static Xml.Document createNewXml(String filePath, @Language("xml") String fileContents) {
        XmlParser parser = new XmlParser();
        Xml.Document brandNewFile = parser.parse(fileContents).findFirst()
                .map(Xml.Document.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Unable to parse XML contents"));
        return brandNewFile.withSourcePath(Paths.get(filePath));
    }

    /**
     * Return true if the `.mvn/extensions.xml` already includes `gradle-enterprise-maven-extension`
     */
    private boolean findExistingEnterpriseExtension(Xml.Document extensionsXml) {
        XPathMatcher xPathMatcher = new XPathMatcher("/extensions/extension/artifactId");
        return new XmlIsoVisitor<AtomicBoolean>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, AtomicBoolean found) {
                if (found.get()) {
                    return tag;
                }
                tag = super.visitTag(tag, found);
                if (xPathMatcher.matches(getCursor())) {
                    Optional<String> maybeArtifactId = tag.getValue();
                    if (maybeArtifactId.isPresent() && maybeArtifactId.get().equals(GRADLE_ENTERPRISE_MAVEN_EXTENSION_ARTIFACT_ID)) {
                        found.set(true);
                    }
                }
                return tag;
            }
        }.reduce(extensionsXml, new AtomicBoolean()).get();
    }

    /**
     * Add `gradle-enterprise-maven-extension` to the file `.mvn/extensions.xml`,
     * this method assumes that `gradle-enterprise-maven-extension` does not exist yet, and it should have been checked.
     */
    private Xml.Document addEnterpriseExtension(Xml.Document extensionsXml, ExecutionContext ctx) {
        @Language("xml")
        String tagSource = version != null ? String.format(ENTERPRISE_TAG_FORMAT, version) : String.format(ENTERPRISE_TAG_FORMAT, getLatestVersion(ctx));
        AddToTagVisitor<ExecutionContext> addToTagVisitor = new AddToTagVisitor<>(
                extensionsXml.getRoot(),
                Xml.Tag.build(tagSource));
        return (Xml.Document) addToTagVisitor.visitNonNull(extensionsXml, ctx);
    }

    private String getLatestVersion(ExecutionContext ctx) {
        MavenPomDownloader pomDownloader = new MavenPomDownloader(Collections.emptyMap(), ctx, null, null);
        VersionComparator versionComparator = new LatestRelease(null);
        GroupArtifact gradleEnterpriseExtension = new GroupArtifact("com.gradle", "gradle-enterprise-maven-extension");
        try {
            MavenMetadata extensionMetadata = pomDownloader.downloadMetadata(gradleEnterpriseExtension, null, Collections.singletonList(MavenRepository.MAVEN_CENTRAL));
        return extensionMetadata.getVersioning()
            .getVersions()
            .stream()
            .filter(v -> versionComparator.isValid(null, v))
            .max((v1, v2) -> versionComparator.compare(null, v1, v2))
            .orElseThrow(() -> new IllegalStateException("Expected to find at least one Gradle Enterprise Maven extension version to select from."));
        } catch (MavenDownloadingException e) {
            throw new IllegalStateException("Could not download Maven metadata", e);
        }
    }
}
