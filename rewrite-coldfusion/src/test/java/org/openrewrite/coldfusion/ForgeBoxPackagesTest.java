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
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ForgeBox's packages served by a stand-in for ForgeBox and GitHub.
 */
class ForgeBoxPackagesTest {
    private static final String REGISTRY = "https://forgebox.example/api/v1";

    private final Map<String, byte[]> served = new HashMap<>();
    private final Map<String, String> redirects = new HashMap<>();

    private final HttpSender http = request -> {
        String url = request.getUrl().toString();
        if (redirects.containsKey(url)) {
            return new HttpSender.Response(301, null, Map.of("Location", List.of(redirects.get(url))), () -> {
            });
        }
        byte[] body = served.get(url);
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

    @Test
    void aVersionNoSemanticVersionRangeNamesInstallsByItsNumber(@TempDir Path cache) throws IOException {
        entry("cbmongodb", "3.5.0.1+132", "https://downloads.example/cbmongodb-3.5.0.1.zip");
        served.put("https://downloads.example/cbmongodb-3.5.0.1.zip", zip("models/Mongo.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("cbmongodb@3.5.0.1");
        assertThat(installed.getVersion()).isEqualTo("3.5.0.1+132");
        assertThat(installed.getDirectory().resolve("models/Mongo.cfc")).isRegularFile();
    }

    @Test
    void aRangeCommandBoxCannotReadIsNoVersion(@TempDir Path cache) {
        entry("cbauth", "6.0.0", "https://downloads.example/cbauth-6.0.0.zip");
        assertThatThrownBy(() -> new ForgeBoxPackages(http, REGISTRY, cache).install("cbauth@1.4.8.0"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aDownloadRedirectedFromHttpToHttpsIsFollowed(@TempDir Path cache) throws IOException {
        entry("cfcouchbase", "3.0.2+47", "http://downloads.example/cfcouchbase-3.0.2.zip");
        redirects.put("http://downloads.example/cfcouchbase-3.0.2.zip", "https://downloads.example/cfcouchbase-3.0.2.zip");
        served.put("https://downloads.example/cfcouchbase-3.0.2.zip", zip("models/Couchbase.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("cfcouchbase@3.0.2");
        assertThat(installed.getDirectory().resolve("models/Couchbase.cfc")).isRegularFile();
    }

    @Test
    void aDownloadFromForgeBoxStorageIsNamedInAnyCase(@TempDir Path cache) throws IOException {
        entry("cbsheets", "1.2.1", "ForgeboxStorage");
        served.put(REGISTRY + "/storage/cbsheets/1.2.1",
          "{\"error\": false, \"data\": \"https://downloads.example/cbsheets-1.2.1.zip\"}".getBytes(StandardCharsets.UTF_8));
        served.put("https://downloads.example/cbsheets-1.2.1.zip", zip("models/Sheet.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("cbsheets@1.2.1");
        assertThat(installed.getDirectory().resolve("models/Sheet.cfc")).isRegularFile();
    }

    @Test
    void anEmptyDirectoryOrPackageDirectoryIsNone(@TempDir Path cache) throws IOException {
        // CommandBox's box.json template writes both empty
        entry("cbpager", "2.8.3", "https://downloads.example/cbpager-2.8.3.zip");
        served.put("https://downloads.example/cbpager-2.8.3.zip", zip(
          "box.json", "{\"slug\": \"cbpager\", \"type\": \"modules\", \"directory\": \"\", \"packageDirectory\": \"\"}",
          "models/Pager.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("cbpager@2.8.3");
        assertThat(installed.getInstallPath()).isEqualTo("modules/cbpager");
    }

    @Test
    void aPackageInAFolderOfItsArchiveInstallsFromThatFolder(@TempDir Path cache) throws IOException {
        entry("specs", "4.5.0+5", "https://downloads.example/specs-4.5.0.zip");
        served.put("https://downloads.example/specs-4.5.0.zip", zip(
          "specs/box.json", "{\"slug\": \"specs\", \"type\": \"testing\"}",
          "specs/system/BaseSpec.cfc", "component {}"));
        ForgeBoxPackages.Installed installed = new ForgeBoxPackages(http, REGISTRY, cache).install("specs@4.5.0");
        assertThat(installed.getInstallPath()).isEqualTo("specs");
        assertThat(installed.getDirectory().resolve("system/BaseSpec.cfc")).isRegularFile();
    }

    @Test
    void aContentBoxModuleInstallsAmongContentBoxsUserModules(@TempDir Path cache) throws IOException {
        entry("cbgallery", "2.0.0", "https://downloads.example/cbgallery-2.0.0.zip");
        served.put("https://downloads.example/cbgallery-2.0.0.zip", zip(
          "box.json", "{\"slug\": \"cbgallery\", \"type\": \"contentbox-modules\"}",
          "ModuleConfig.cfc", "component {}"));
        assertThat(new ForgeBoxPackages(http, REGISTRY, cache).install("cbgallery@2.0.0").getInstallPath())
          .isEqualTo("modules/contentbox/modules_user/cbgallery");
    }

    @Test
    void aWidgetIsDroppedIntoContentBoxsWidgetsWithoutAFolderOfItsOwn(@TempDir Path cache) throws IOException {
        entry("cbwidget-clock", "1.0.0", "https://downloads.example/cbwidget-clock-1.0.0.zip");
        served.put("https://downloads.example/cbwidget-clock-1.0.0.zip", zip(
          "box.json", "{\"slug\": \"cbwidget-clock\", \"type\": \"contentbox-widgets\", \"createPackageDirectory\": true}",
          "Clock.cfc", "component {}"));
        assertThat(new ForgeBoxPackages(http, REGISTRY, cache).install("cbwidget-clock@1.0.0").getInstallPath())
          .isEqualTo("modules/contentbox/widgets");
    }

    @Test
    void aPackageInstallsInAFolderNamedForTheSlugItsBoxJsonGives(@TempDir Path cache) throws IOException {
        entry("old-sheets", "3.0.0", "https://downloads.example/old-sheets-3.0.0.zip");
        served.put("https://downloads.example/old-sheets-3.0.0.zip", zip(
          "box.json", "{\"slug\": \"sheets\", \"type\": \"modules\"}",
          "Sheet.cfc", "component {}"));
        assertThat(new ForgeBoxPackages(http, REGISTRY, cache).install("old-sheets@3.0.0").getInstallPath())
          .isEqualTo("modules/sheets");
    }
}
