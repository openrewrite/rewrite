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

	// Test created to reproduce the error reported within the PR submitted
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
    }
}
