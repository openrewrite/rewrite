/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.coldfusion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ipc.http.HttpSender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ForgeBox's packages served by a stand-in for ForgeBox and GitHub.
 */
class ForgeBoxPackagesTest {
    private static final String REGISTRY = "https://forgebox.example/api/v1";

    private final Map<String, byte[]> served = new HashMap<>();

    private final HttpSender http = request -> {
        byte[] body = served.get(request.getUrl().toString());
        return new HttpSender.Response(body == null ? 404 : 200, body == null ? null : new ByteArrayInputStream(body), () -> {
        });
    };

    private void entry(String slug, String version, String downloadURL) {
        served.put(REGISTRY + "/entry/" + slug, ("{\"error\": false, \"data\": {\"slug\": \"" + slug + "\", " +
                                                 "\"typeSlug\": \"modules\", \"versions\": [{\"version\": \"" + version +
                                                 "\", \"downloadURL\": \"" + downloadURL + "\"}]}}")
          .getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] zip(String... namesAndContents) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[i]));
                zip.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Test
    void aGitHubRepositoryInstallsFromGitHubsArchiveOfItsRef(@TempDir Path cache) throws IOException {
        // ForgeBox gives some packages' downloads as CommandBox's shorthand for a GitHub repository
        entry("normalizeToArray", "1.0.3", "elpete/normalizeToArray#v1.0.3");
        served.put("https://github.com/elpete/normalizeToArray/archive/v1.0.3.zip", zip(
          "normalizeToArray-1.0.3/", "",
          "normalizeToArray-1.0.3/box.json", "{\"slug\": \"normalizeToArray\"}",
          "normalizeToArray-1.0.3/models/Normalizer.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("normalizeToArray@1.0.3");
        assertThat(installed.getVersion()).isEqualTo("1.0.3");
        assertThat(installed.getInstallPath()).isEqualTo("modules/normalizeToArray");
        // the archive's own directory is left out, as a clone has none
        assertThat(installed.getDirectory().resolve("box.json")).isRegularFile();
        assertThat(installed.getDirectory().resolve("models/Normalizer.cfc")).isRegularFile();
    }

    @Test
    void aGitHubRepositoryWithoutARefIsItsMasterElseItsMain(@TempDir Path cache) throws IOException {
        entry("pkce", "2.0.0", "github:someone/pkce");
        served.put("https://github.com/someone/pkce/archive/main.zip", zip("pkce-main/models/Pkce.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("pkce");
        assertThat(installed.getDirectory().resolve("models/Pkce.cfc")).isRegularFile();
    }
}
