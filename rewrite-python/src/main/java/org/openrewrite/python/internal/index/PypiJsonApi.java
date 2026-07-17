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
package org.openrewrite.python.internal.index;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.PythonPackageIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opportunistic hash collection from the PyPI JSON API, matching pipenv's special-casing.
 * Only valid for indexes hosted on pypi.org; returns null on any failure so callers
 * always fall back to the Simple API.
 */
public class PypiJsonApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private PypiJsonApi() {
    }

    public static boolean isPypi(PythonPackageIndex index) {
        String host = Urls.host(index.getUrl());
        if (host == null) {
            return false;
        }
        String lower = host.toLowerCase(Locale.ROOT);
        return "pypi.org".equals(lower) || lower.endsWith(".pypi.org");
    }

    public static @Nullable List<String> sha256Digests(HttpSender httpSender, String name, String version) {
        return sha256Digests(httpSender, "https://pypi.org", name, version);
    }

    static @Nullable List<String> sha256Digests(HttpSender httpSender, String baseUrl, String name, String version) {
        String url = baseUrl + "/pypi/" + name + "/" + version + "/json";
        try (HttpSender.Response response = httpSender.send(httpSender.get(url).build())) {
            if (!response.isSuccessful()) {
                return null;
            }
            Release release = MAPPER.readValue(response.getBodyAsBytes(), Release.class);
            if (release.urls == null) {
                return null;
            }
            List<String> digests = new ArrayList<>();
            for (ReleaseFile file : release.urls) {
                if (file.digests != null && file.digests.sha256 != null) {
                    digests.add(file.digests.sha256);
                }
            }
            return digests;
        } catch (Exception e) {
            return null;
        }
    }

    private static class Release {
        public @Nullable List<ReleaseFile> urls;
    }

    private static class ReleaseFile {
        public @Nullable Digests digests;
    }

    private static class Digests {
        public @Nullable String sha256;
    }
}
