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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.SAME_THREAD)
public class MavenSettingsWithServerHttpHeaderParsedByJacksonTest {

    private String originalUserHome;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

	@Test
	void serverHttpHeaders() throws IOException {
		var settingsXml = """
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

        XmlMapper xmlMapper = new XmlMapper();
        MavenSettings settings = xmlMapper.readValue(settingsXml, MavenSettings.class);

        Assertions.assertNotNull(settings.getServers());
        MavenSettings.Server server = settings.getServers().getServers().getFirst();

        Assertions.assertNotNull(server.getConfiguration().getHttpHeaders());
        assertThat(server.getConfiguration().getHttpHeaders().getFirst().getName()).isEqualTo("X-JFrog-Art-Api");
	}
}
