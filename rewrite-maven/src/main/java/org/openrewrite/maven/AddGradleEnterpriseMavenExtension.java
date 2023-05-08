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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Value
@EqualsAndHashCode(callSuper = true)
public class AddGradleEnterpriseMavenExtension extends ScanningRecipe<AddGradleEnterpriseMavenExtension.Accumulator> {
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

    @Language("xml")
    private static final String GRADLE_ENTERPRISE_XML_FORMAT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n" +
            "<gradleEnterprise\n" +
            "    xmlns=\"https://www.gradle.com/gradle-enterprise-maven\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "    xsi:schemaLocation=\"https://www.gradle.com/gradle-enterprise-maven" +
            " https://www.gradle.com/schema/gradle-enterprise-maven.xsd\">\n" +
            "  <server>\n" +
            "    <url>%s</url>\n" +
            "    <allowUntrusted>%b</allowUntrusted>\n" +
            "  </server>\n" +
            "  <buildScan>\n" +
            "    <backgroundBuildScanUpload>false</backgroundBuildScanUpload>\n" +
            "    <publish>ALWAYS</publish>\n" +
            "  </buildScan>\n" +
            "</gradleEnterprise>";

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
            description = "When set to `true` the plugin will be configured to allow unencrypted http connections with the server. " +
                    "If set to `false` or omitted, the plugin will refuse to communicate without transport layer security enabled.",
            required = false,
            example = "true")
    @Nullable
    Boolean allowUntrustedServer;

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

    @Data
    static class Accumulator {
        boolean mavenProject;
        Path matchingExtensionsXmlFile;
        Path matchingGradleEnterpriseXmlFile;
    }

    @Override
    public Accumulator getInitialValue() {
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
                String sourcePath = sourceFile.getSourcePath().toString();
                switch (sourcePath) {
                    case "pom.xml":
                        acc.setMavenProject(true);
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
        sources.add(createNewXml(GRADLE_ENTERPRISE_XML_PATH,
                String.format(GRADLE_ENTERPRISE_XML_FORMAT, server, allowUntrustedServer != null ? allowUntrustedServer : Boolean.FALSE)));

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

    private static Xml.Document createNewXml(String filePath, @Language("xml") String fileContents) {
        XmlParser parser = new XmlParser();
        Xml.Document brandNewFile = parser.parse(fileContents).findFirst().get();
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
