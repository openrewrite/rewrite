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
