package org.openrewrite.ge;

import org.openrewrite.RefactorModule;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.List;

import static java.util.Arrays.asList;

public class AddGradleEnterpriseToMavenProject implements RefactorModule<Xml.Document, Xml> {
    private final Configuration configuration;

    public AddGradleEnterpriseToMavenProject(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<Xml.Document> getDeclaredOutputs() {
        Xml.Document extensionsXml = new XmlParser().parseFromString(Path.of(".mvn/extensions.xml"),
                "<extensions>\n" +
                        "  <extension>\n" +
                        "    <groupId>com.gradle</groupId>\n" +
                        "    <artifactId>gradle-enterprise-maven-extension</artifactId>\n" +
                        "    <version>" + configuration.getExtensionVersion() + "</version>\n" +
                        "  </extension>\n" +
                        "</extensions>\n");

        Xml.Document gradleEnterpriseXml = new XmlParser().parseFromString(Path.of("gradle-enterprise.xml"),
                "<gradleEnterprise>\n" +
                        "  <server>\n" +
                        "    <url>" + configuration.getGradleEnterpriseServer() + "</url>\n" +
                        "  </server>\n" +
                        "  <buildScan>\n" +
                        "    <termsOfService>\n" +
                        "      <url>https://gradle.com/terms-of-service</url>\n" +
                        "      <accept>true</accept>\n" +
                        "    </termsOfService>\n" +
                        "  </buildScan>\n" +
                        "</gradleEnterprise>\n");

        return asList(extensionsXml, gradleEnterpriseXml);
    }
}
