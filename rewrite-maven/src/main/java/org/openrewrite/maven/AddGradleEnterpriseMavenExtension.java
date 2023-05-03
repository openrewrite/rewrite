package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.xml.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


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
    private static final String EXTENSION_TAG_FORMAT = "<extension>\n" +
                                                       "  <groupId>com.gradle</groupId>\n" +
                                                       "  <artifactId>gradle-enterprise-maven-extension</artifactId>\n" +
                                                       "  <version>%s</version>\n" +
                                                       "</extension>";

    @Language("xml")
    private static final String EXTENSION_TAG_FORMAT_WITHOUT_VERSION = "<extension>\n" +
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
        example = "3.x")
    @Nullable
    String version;

    @Option(displayName = "Server URL",
        description = "The URL of the Gradle Enterprise server.",
        example = "https://ge.openrewrite.org/")
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
        return "To integrate Gradle Enterprise Maven Extension into Maven projects, ensure that the " +
               "`gradle-enterprise-maven-extension` is added to the `.mvn/extensions.xml` file if not already present. " +
               "Additionally, configure the extension by adding the `.mvn/gradle-enterprise.xml` configuration file.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        boolean isMavenProject = false;
        AtomicReference<SourceFile> matchingExtensionsXmlFile = new AtomicReference<>();
        AtomicReference<SourceFile> matchingGradleEnterpriseXmlFile = new AtomicReference<>();

        for (SourceFile sourceFile : before) {
            String sourcePath = sourceFile.getSourcePath().toString();
            switch (sourcePath) {
                case "pom.xml":
                    isMavenProject = true;
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

        Xml.Document gradleEnterpriseXml = createNewXml(GRADLE_ENTERPRISE_XML_PATH,
            String.format(GRADLE_ENTERPRISE_XML_FORMAT, server, allowUntrustedServer != null ? allowUntrustedServer : Boolean.FALSE));

        if (matchingExtensionsXmlFile.get() != null) {
            if (!(matchingExtensionsXmlFile.get() instanceof Xml.Document)) {
                throw new RuntimeException("The extensions.xml is not xml document type");
            }

            Xml.Document extensionsXml = ( Xml.Document) matchingExtensionsXmlFile.get();

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

    private static Xml.Document createNewXml(String filePath, String fileContents) {
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
        String tagSource = version != null ? String.format(EXTENSION_TAG_FORMAT, version) : EXTENSION_TAG_FORMAT_WITHOUT_VERSION;
        AddToTagVisitor<ExecutionContext> addToTagVisitor = new AddToTagVisitor<>(
            extensionsXml.getRoot(),
            Xml.Tag.build(tagSource));
        return (Xml.Document) addToTagVisitor.visit(extensionsXml, ctx);
    }
}
