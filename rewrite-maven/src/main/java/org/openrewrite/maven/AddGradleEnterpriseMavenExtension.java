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
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenXmlMapper;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.xml.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.PathUtils.separatorsToUnix;


@Value
@EqualsAndHashCode(callSuper = true)
public class AddGradleEnterpriseMavenExtension extends Recipe {
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

    @Language("xml")
    private static final String ENTERPRISE_TAG_FORMAT_WITHOUT_VERSION = "<extension>\n" +
            "  <groupId>com.gradle</groupId>\n" +
            "  <artifactId>gradle-enterprise-maven-extension</artifactId>\n" +
            "</extension>";

    @Option(displayName = "Plugin version",
            description = "An exact version number or node-style semver selector used to select the gradle-enterprise-maven-extension version.",
            example = "1.x")
    @Nullable
    String version;

    @Option(displayName = "Server URL",
            description = "The URL of the Gradle Enterprise server.",
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
            description = "When set to `always` the extension will publish build scans of every single build. " +
                    "This is the default behavior when omitted." +
                    "When set to `failure` the extension will only publish build scans when the build fails. " +
                    "When set to `demand` the extension will only publish build scans when explicitly requested.",
            required = false,
            valid = {"always", "failure", "demand"},
            example = "true")
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
        return "Add Gradle Enterprise Maven Extension to maven projects";
    }

    @Override
    public String getDescription() {
        return "To integrate gradle enterprise maven extension into maven projects, ensure that the " +
                "`gradle-enterprise-maven-extension` is added to the `.mvn/extensions.xml` file if not already present. " +
                "Additionally, configure the extension by adding the `.mvn/gradle-enterprise.xml` configuration file.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        boolean isMavenProject = false;
        boolean useCRLFNewLines = false;
        AtomicReference<SourceFile> matchingExtensionsXmlFile = new AtomicReference<>();
        AtomicReference<SourceFile> matchingGradleEnterpriseXmlFile = new AtomicReference<>();

        for (SourceFile sourceFile : before) {
            String sourcePath = separatorsToUnix(sourceFile.getSourcePath().toString());
            switch (sourcePath) {
                case "pom.xml":
                    isMavenProject = true;
                    useCRLFNewLines = sourceFile.getStyle(GeneralFormatStyle.class, new GeneralFormatStyle(false))
                            .isUseCRLFNewLines();
                    break;
                case EXTENSIONS_XML_PATH:
                    matchingExtensionsXmlFile.set(sourceFile);
                    break;
                case GRADLE_ENTERPRISE_XML_PATH:
                    matchingGradleEnterpriseXmlFile.set(sourceFile);
                    break;
                default:
                    break;
            }
        }

        // This recipe makes change for maven project only, or if the file `.mvn/gradle-enterprise.xml` already exists, do nothing
        if (!isMavenProject || matchingGradleEnterpriseXmlFile.get() != null) {
            return before;
        }
        Xml.Document gradleEnterpriseXml = createNewXml(GRADLE_ENTERPRISE_XML_PATH, gradleEnterpriseConfiguration(useCRLFNewLines));

        if (matchingExtensionsXmlFile.get() != null) {
            if (!(matchingExtensionsXmlFile.get() instanceof Xml.Document)) {
                throw new RuntimeException("The extensions.xml is not xml document type");
            }

            Xml.Document extensionsXml = (Xml.Document) matchingExtensionsXmlFile.get();

            // find `gradle-enterprise-maven-extension` extension, do nothing if it already exists,
            boolean hasEnterpriseExtension = findExistingEnterpriseExtension(extensionsXml);
            if (hasEnterpriseExtension) {
                return before;
            }

            Xml.Document updatedExtensionsXml = addEnterpriseExtension(extensionsXml, ctx);
            before = ListUtils.map(before, s -> s == extensionsXml ? updatedExtensionsXml : s);
            return ListUtils.concat(before, gradleEnterpriseXml);
        }

        Xml.Document extensionsXml = createNewXml(EXTENSIONS_XML_PATH, EXTENSIONS_XML_FORMAT);
        extensionsXml = addEnterpriseExtension(extensionsXml, ctx);
        return ListUtils.concat(ListUtils.concat(before, extensionsXml), gradleEnterpriseXml);
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
        Xml.Document brandNewFile = parser.parse(fileContents).get(0);
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
        String tagSource = version != null ? String.format(ENTERPRISE_TAG_FORMAT, version) : ENTERPRISE_TAG_FORMAT_WITHOUT_VERSION;
        AddToTagVisitor<ExecutionContext> addToTagVisitor = new AddToTagVisitor<>(
                extensionsXml.getRoot(),
                Xml.Tag.build(tagSource));
        return (Xml.Document) addToTagVisitor.visitNonNull(extensionsXml, ctx);
    }
}
