/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.assertThat;

class MavenSettingsSecurityTest {
    @TempDir Path tempDir;

    private String originalUserHome;
    private final String MASTER_PASS_ENCRYPTED = "FyoLIiN2Fx8HpT8O0aBsTn2/s3pYmtLRRCpoWPzhN4A=";
//    private final String MASTER_PASS_DECRYPTED = "master";
    private final String USER_PASS_ENCRYPTED = "ERozWEamSJoHRBT+wVx51V2Emr9PazZR9txMntZPlJc=";
    private final String USER_PASS_DECRYPTED = "testpass";

    @BeforeEach
    void setUp() {
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void decryptCredentials() throws IOException {
        // Create settings-security.xml with master password
        Path securitySettingsPath = tempDir.resolve(".m2/settings-security.xml");
        Files.createDirectories(securitySettingsPath.getParent());
        Files.write(
          securitySettingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settingsSecurity>
                  <master>{%s}</master>
              </settingsSecurity>
              """.formatted(MASTER_PASS_ENCRYPTED)
          ).getBytes(), StandardOpenOption.CREATE
        );

        // Create settings.xml with encrypted password
        Path settingsPath = tempDir.resolve(".m2/settings.xml");
        Files.write(
          settingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settings>
                  <servers>
                      <server>
                          <id>test-server</id>
                          <username>admin</username>
                          <password>{%s}</password>
                      </server>
                  </servers>
              </settings>
              """.formatted(USER_PASS_ENCRYPTED)
          ).getBytes(), StandardOpenOption.CREATE
        );

        var ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });

        // Use the public API to read settings
        MavenSecuritySettings securitySettings = MavenSecuritySettings.readMavenSecuritySettingsFromDisk(ctx);
        MavenSettings settings = MavenSettings.readMavenSettingsFromDisk(ctx);

        assertThat(securitySettings).isNotNull();
        assertThat(securitySettings.getMaster()).isNotNull();

        assertThat(settings).isNotNull();
        assertThat(settings.getServers()).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1).first().satisfies(server -> {
            assertThat(server.getId()).isEqualTo("test-server");
            assertThat(server.getUsername()).isEqualTo("admin");
            assertThat(server.getPassword()).isEqualTo(USER_PASS_DECRYPTED);
        });
    }

    @Test
    void handleInvalidEncryptedPassword() throws IOException {
        // Create settings-security.xml with master password
        Path securitySettingsPath = tempDir.resolve(".m2/settings-security.xml");
        Files.createDirectories(securitySettingsPath.getParent());
        Files.write(
          securitySettingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settingsSecurity>
                  <master>{jSMOWnoPFgsHVpMvz5VrIt5kRbzGpI8u+9EF1iFQyJQ=}</master>
              </settingsSecurity>
              """
          ).getBytes(), StandardOpenOption.CREATE
        );

        // Create settings.xml with invalid encrypted password
        Path settingsPath = tempDir.resolve(".m2/settings.xml");
        Files.write(
          settingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settings>
                  <servers>
                      <server>
                          <id>test-server</id>
                          <username>admin</username>
                          <password>{invalid_format}</password>
                      </server>
                  </servers>
              </settings>
              """
          ).getBytes(), StandardOpenOption.CREATE
        );

        var ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });

        MavenSettings settings = MavenSettings.readMavenSettingsFromDisk(ctx);

        assertThat(settings).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1).first().satisfies(server -> {
            assertThat(server.getPassword()).isEqualTo("{invalid_format}");
        });
    }

    @Test
    void noSecuritySettings() throws IOException {
        // Only create settings.xml without settings-security.xml
        Path settingsPath = tempDir.resolve(".m2/settings.xml");
        Files.createDirectories(settingsPath.getParent());
        Files.write(
          settingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settings>
                  <servers>
                      <server>
                          <id>test-server</id>
                          <username>admin</username>
                          <password>{encrypted_password}</password>
                      </server>
                  </servers>
              </settings>
              """
          ).getBytes(), StandardOpenOption.CREATE
        );

        var ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });

        MavenSettings settings = MavenSettings.readMavenSettingsFromDisk(ctx);

        assertThat(settings).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1).first().satisfies(server -> {
            assertThat(server.getPassword()).isEqualTo("{encrypted_password}");
        });
    }

    @Test
    void decryptPasswordWithComments() throws IOException {
        // Create settings-security.xml with master password
        Path securitySettingsPath = tempDir.resolve(".m2/settings-security.xml");
        Files.createDirectories(securitySettingsPath.getParent());
        Files.write(
          securitySettingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settingsSecurity>
                  <master>{%s}</master>
              </settingsSecurity>
              """.formatted(MASTER_PASS_ENCRYPTED)
          ).getBytes(), StandardOpenOption.CREATE
        );

        // Create settings.xml with password containing comments
        Path settingsPath = tempDir.resolve(".m2/settings.xml");
        Files.write(
          settingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settings>
                  <servers>
                      <server>
                          <id>test-server</id>
                          <username>admin</username>
                          <password>Oleg reset this password on 2009-03-11, expires on 2009-04-11 {%s}</password>
                      </server>
                  </servers>
              </settings>
              """.formatted(USER_PASS_ENCRYPTED)
          ).getBytes(), StandardOpenOption.CREATE
        );

        var ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });

        MavenSettings settings = MavenSettings.readMavenSettingsFromDisk(ctx);

        assertThat(settings).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1).first().satisfies(server -> {
            assertThat(server.getPassword()).isEqualTo(USER_PASS_DECRYPTED);
        });
    }

    @Test
    void invalidMasterPasswordButValidPasswordFormat() throws IOException {
        // Create settings-security.xml with invalid master password
        Path securitySettingsPath = tempDir.resolve(".m2/settings-security.xml");
        Files.createDirectories(securitySettingsPath.getParent());
        Files.write(
          securitySettingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settingsSecurity>
                  <master>{invalid_master_password}</master>
              </settingsSecurity>
              """
          ).getBytes(), StandardOpenOption.CREATE
        );

        // Create settings.xml with valid encrypted password
        Path settingsPath = tempDir.resolve(".m2/settings.xml");
        Files.write(
          settingsPath, (
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <settings>
                  <servers>
                      <server>
                          <id>test-server</id>
                          <username>admin</username>
                          <password>{%s}</password>
                      </server>
                  </servers>
              </settings>
              """.formatted(USER_PASS_ENCRYPTED)
          ).getBytes(), StandardOpenOption.CREATE
        );

        var ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });

        MavenSettings settings = MavenSettings.readMavenSettingsFromDisk(ctx);

        assertThat(settings).isNotNull();
        assertThat(settings.getServers().getServers()).hasSize(1).first().satisfies(server -> {
            // Password should remain in encrypted form since master password is invalid
            assertThat(server.getPassword()).isEqualTo("{%s}".formatted(USER_PASS_ENCRYPTED));
        });
    }
}
