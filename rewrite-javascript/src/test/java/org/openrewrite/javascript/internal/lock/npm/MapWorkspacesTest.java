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
package org.openrewrite.javascript.internal.lock.npm;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;

/**
 * MapWorkspaces against what @npmcli/map-workspaces found in the same directory layouts (the
 * minimatch-cases fixture).
 */
class MapWorkspacesTest {

    private static final JsonNode CASES = fixture("minimatch-cases");
    private static final String CWD = "/project";

    static Stream<Arguments> cases() {
        List<Arguments> out = new ArrayList<>();
        for (JsonNode layout : CASES.get("layouts")) {
            for (JsonNode c : layout.get("cases")) {
                out.add(Arguments.of(layout.get("name").asText(), c.get("workspaces").toString(),
                                     layout.get("manifests"), c));
            }
        }
        return out.stream();
    }

    private static VirtualTreeLoader.ProjectFiles manifests(JsonNode declared) {
        Map<String, String> manifests = new LinkedHashMap<>();
        declared.fields().forEachRemaining(e -> manifests.put(e.getKey(), e.getValue().asText()));
        return VirtualTreeLoader.ProjectFiles.builder().manifests(manifests).build();
    }

    private static List<List<String>> relative(Map<?, String> members) {
        List<List<String>> out = new ArrayList<>();
        for (Map.Entry<?, String> e : members.entrySet()) {
            List<String> pair = new ArrayList<>();
            pair.add(String.valueOf(e.getKey()));
            pair.add(Relpath.relpath(CWD, e.getValue()));
            out.add(pair);
        }
        return out;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("cases")
    void mapWorkspacesOverManifests(String layout, String workspaces, JsonNode declared, JsonNode c) {
        Map<String, Object> pkg = obj("workspaces", json(c.get("workspaces")));
        if (c.has("error")) {
            String message = c.get("error").get("message").asText().replace("{CWD}", CWD);
            if ("EJSONPARSE".equals(c.get("error").path("code").asText())) {
                assertThatThrownBy(() -> MapWorkspaces.mapWorkspaces(CWD, pkg, manifests(declared)))
                  .isInstanceOf(JsonParseEvenBetterErrors.JsonParseException.class)
                  .hasMessageStartingWith("Invalid package.json: ");
            } else {
                assertThatThrownBy(() -> MapWorkspaces.mapWorkspaces(CWD, pkg, manifests(declared)))
                  .hasMessage(message);
            }
        } else {
            assertThat(relative(MapWorkspaces.mapWorkspaces(CWD, pkg, manifests(declared))))
              .isEqualTo(json(c.get("members")));
        }
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("cases")
    void virtualOverLock(String layout, String workspaces, JsonNode declared, JsonNode c) {
        Map<String, Object> packages = obj("", obj("workspaces", json(c.get("workspaces"))));
        declared.fields().forEachRemaining(e -> {
            String p = e.getKey();
            Object pkg = parsed(e.getValue().asText());
            if (!p.isEmpty() && pkg instanceof Map) {
                packages.put(p, pkg);
                Object name = ((Map<?, ?>) pkg).get("name");
                packages.put("node_modules/" + (name != null ? name : PosixPath.basename(p)), obj("resolved", p, "link",
                  true));
            }
        });
        Map<String, Object> lockfile = obj("packages", packages);
        if (c.has("virtualError")) {
            assertThatThrownBy(() -> MapWorkspaces.virtual(CWD, lockfile)).hasMessage(c.get("virtualError")
              .get("message").asText());
        } else {
            assertThat(relative(MapWorkspaces.virtual(CWD, lockfile))).isEqualTo(json(c.get("virtual")));
        }
    }

    private static Object parsed(String text) {
        try {
            return JsonParseEvenBetterErrors.parse(text).getValue();
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return text;
        }
    }
}
