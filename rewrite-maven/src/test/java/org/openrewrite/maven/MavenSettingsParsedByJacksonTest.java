/*
 * Copyright 2026 the original author or authors.
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

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.maven.internal.MavenXmlMapper;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.SAME_THREAD)
public class MavenSettingsParsedByJacksonTest {

    String settingsXml = """
<settings>
    <servers>
        <server>
            <id>maven-snapshots</id>
            <configuration>
                <httpHeaders>
                    <property>
                        <name>X-JFrog-Art-Api</name>
                        <value>myApiToken</value>
                    </property>
                </httpHeaders>
            </configuration>
        </server>
    </servers>
    <profiles>
        <profile>
            <id>my-profile</id>
            <repositories>
                <repository>
                    <id>maven-snapshots</id>
                    <name>Private Repo</name>
                    <url>https://repo.company.net/maven</url>
                </repository>
            </repositories>
        </profile>
    </profiles>
</settings>""";

    /* We can fix the issue reported part of the PR: 6543 if we change from "false" to "true" the
       value of the .defaultUseWrapper(true) of the XmlMapper.Builder
       but by doing that we will get errors on the project like:

       Caused by:
       at org.openrewrite.maven.AddDependencyTest.addTestDependenciesAfterCompile(AddDependencyTest.java:531)
       ...
        java.io.UncheckedIOException: Failed to parse pom: Cannot construct instance of `org.openrewrite.maven.internal.RawPom$Dependency` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value ('commons-lang')
         at [Source: (org.openrewrite.internal.EncodingDetectingInputStream); line: 7, column: 34] (through reference chain: org.openrewrite.maven.internal.RawPom["dependencies"]->org.openrewrite.maven.internal.RawPom$Dependencies["dependency"]->java.util.ArrayList[0])
            at app//org.openrewrite.maven.internal.RawPom.parse(RawPom.java:162)
            at app//org.openrewrite.maven.MavenParser.parseInputs(MavenParser.java:75)
            ... 3 more

            Caused by:
            com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot construct instance of `org.openrewrite.maven.internal.RawPom$Dependency` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value ('commons-lang')
             at [Source: (org.openrewrite.internal.EncodingDetectingInputStream); line: 7, column: 34] (through reference chain: org.openrewrite.maven.internal.RawPom["dependencies"]->org.openrewrite.maven.internal.RawPom$Dependencies["dependency"]->java.util.ArrayList[0])
                at app//com.fasterxml.jackson.databind.exc.MismatchedInputException.from(MismatchedInputException.java:63)

           AddDependencyTest > addScopedDependency(String) > [1] scope=provided FAILED
               org.opentest4j.AssertionFailedError: [Unexpected result in "project/pom.xml":
               diff --git a/project/pom.xml b/project/pom.xml
               index 01a8359..ae86eeb 100644
               --- a/project/pom.xml
               +++ b/project/pom.xml
               @@ -3,7 +3,9 @@
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
               -        <dependency>
               +        <!--~~(Unable to download POM: com.fasterxml.jackson.core:jackson-core:2.12.0. Tried repositories:
               +https://repo.maven.apache.org/maven2: Failed to parse pom: Cannot construct instance of `org.openrewrite.maven.internal.RawPom$License` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value ('The Apache Software License, Version 2.0')
               + at [Source: (ByteArrayInputStream); line: 22, column: 53] (through reference chain: org.openrewrite.maven.internal.RawPom["licenses"]->org.openrewrite.maven.internal.RawPom$Licenses["license"]->java.util.ArrayList[0]))~~>--><dependency>
    */
    @Test
    void shouldGetHttpHeaderParsedUsingXmlMapper() throws IOException {

        XMLInputFactory input = new WstxInputFactory();
        input.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        XmlFactory xmlFactory = new XmlFactory(input, new WstxOutputFactory());

        ObjectMapper m = XmlMapper.builder(xmlFactory)
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL)
          .defaultUseWrapper(true) // false to true
          .build();

        m.registerModule(new ParameterNamesModule())
          .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
          .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY))
          .registerModule(new JavaTimeModule())
          .registerModule(new MavenXmlMapper.StringTrimModule());

        MavenSettings settings = m.readValue(settingsXml, MavenSettings.class);

        Assertions.assertNotNull(settings.getServers());
        MavenSettings.Server server = settings.getServers().getServers().getFirst();

        Assertions.assertNotNull(server.getConfiguration().getHttpHeaders());
        //assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isNull();
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isEqualTo("X-JFrog-Art-Api");
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getValue()).isEqualTo("myApiToken");
    }

	// Test created to reproduce the error reported within the PR: 6543
    @Test
	void shouldGetThehttpHeaderParsedButNull() throws IOException {

        XMLInputFactory input = new WstxInputFactory();
        input.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        input.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        XmlFactory xmlFactory = new XmlFactory(input, new WstxOutputFactory());

        ObjectMapper m = XmlMapper.builder(xmlFactory)
          .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
          .enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL)
          .defaultUseWrapper(false)
          .build()
          .registerModule(new ParameterNamesModule())
          .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
          .disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY))
          .registerModule(new JavaTimeModule())
          .registerModule(new MavenXmlMapper.StringTrimModule());

        MavenSettings settings = m.readValue(settingsXml, MavenSettings.class);

        Assertions.assertNotNull(settings.getServers());
        MavenSettings.Server server = settings.getServers().getServers().getFirst();

        Assertions.assertNotNull(server.getConfiguration().getHttpHeaders());
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isNull();
	}

    @Test
    void shouldGetTheHttpHeaderNameAndValue() throws IOException {

        XmlMapper m = new XmlMapper();
        MavenSettings settings = m.readValue(settingsXml, MavenSettings.class);

        Assertions.assertNotNull(settings.getServers());
        MavenSettings.Server server = settings.getServers().getServers().getFirst();

        Assertions.assertNotNull(server.getConfiguration().getHttpHeaders());
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isEqualTo("X-JFrog-Art-Api");
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getValue()).isEqualTo("myApiToken");
    }
}
