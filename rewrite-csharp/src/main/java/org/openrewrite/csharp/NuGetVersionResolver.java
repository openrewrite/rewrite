/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.ipc.http.HttpSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves available NuGet package versions from NuGet V3 package sources
 * using the {@link HttpSender} from the {@link ExecutionContext}.
 */
class NuGetVersionResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NUGET_ORG_FLAT_CONTAINER = "https://api.nuget.org/v3-flatcontainer/";

    static List<String> resolveAvailableVersions(
            String packageName,
            List<MSBuildProject.PackageSource> packageSources,
            ExecutionContext ctx) {

        HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();

        for (MSBuildProject.PackageSource source : packageSources) {
            List<String> versions = fetchVersionsFromSource(packageName, source.getUrl(), httpSender);
            if (!versions.isEmpty()) {
                return versions;
            }
        }

        return fetchVersionsFromFlatContainer(packageName, NUGET_ORG_FLAT_CONTAINER, httpSender);
    }

    private static List<String> fetchVersionsFromSource(String packageName, String sourceUrl, HttpSender httpSender) {
        try {
            String flatContainerUrl = resolveFlatContainerUrl(sourceUrl, httpSender);
            if (flatContainerUrl == null) {
                return Collections.emptyList();
            }
            return fetchVersionsFromFlatContainer(packageName, flatContainerUrl, httpSender);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static List<String> fetchVersionsFromFlatContainer(String packageName, String flatContainerBaseUrl, HttpSender httpSender) {
        try {
            String url = flatContainerBaseUrl;
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += packageName.toLowerCase() + "/index.json";

            JsonNode response = httpGetJson(url, httpSender);
            if (response == null || !response.has("versions")) {
                return Collections.emptyList();
            }

            List<String> versions = new ArrayList<>();
            for (JsonNode version : response.get("versions")) {
                versions.add(version.asText());
            }
            return versions;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static @Nullable String resolveFlatContainerUrl(String serviceIndexUrl, HttpSender httpSender) {
        try {
            JsonNode index = httpGetJson(serviceIndexUrl, httpSender);
            if (index == null || !index.has("resources")) {
                return null;
            }

            for (JsonNode resource : index.get("resources")) {
                String type = resource.has("@type") ? resource.get("@type").asText() : "";
                if (type.startsWith("PackageBaseAddress")) {
                    return resource.get("@id").asText();
                }
            }
        } catch (Exception e) {
            // Fall through
        }
        return null;
    }

    private static @Nullable JsonNode httpGetJson(String url, HttpSender httpSender) {
        try (HttpSender.Response response = httpSender.get(url).withHeader("Accept", "application/json").send()) {
            if (response.getCode() != 200) {
                return null;
            }
            return MAPPER.readTree(response.getBody());
        } catch (Exception e) {
            return null;
        }
    }
}
