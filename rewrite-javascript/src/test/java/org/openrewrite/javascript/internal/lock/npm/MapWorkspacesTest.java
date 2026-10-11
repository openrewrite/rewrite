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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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

    // brace-expansion 5.0.9, which npm bundles from 11.19.1 and npm 12 from 12.1.0, combines the sets of a pattern
    // left to right: {x,y}{a},} then also names x and y (5.0.6, 5.0.7 and 5.0.9 run on the pattern)
    @Test
    void patternExpandsAsTheBraceExpansionOfTheReleaseDoes() {
        Map<String, Object> packages = obj(
          "", obj("workspaces", new ArrayList<>(singletonList("packages/{x,y}{a},}"))),
          "packages/x", obj("name", "x"),
          "node_modules/x", obj("resolved", "packages/x", "link", true));
        Map<String, Object> lockfile = obj("packages", packages);
        for (String version : new String[]{"11.17.0", "11.18.0", "11.19.0", "12.0.0", "12.0.2"}) {
            assertThat(MapWorkspaces.virtual(CWD, lockfile, NpmRelease.of(version))).as("npm %s", version).isEmpty();
        }
        for (String version : new String[]{"11.19.1", "11.21.0", "12.1.0", "12.2.0"}) {
            assertThat(relative(MapWorkspaces.virtual(CWD, lockfile, NpmRelease.of(version)))).as("npm %s", version)
              .containsExactly(asList("x", "packages/x"));
        }
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
                assertThatThrownBy(() -> MapWorkspaces.mapWorkspaces(CWD, pkg, manifests(declared), NpmRelease.PORTED))
                  .isInstanceOf(JsonParseEvenBetterErrors.JsonParseException.class)
                  .hasMessageStartingWith("Invalid package.json: ");
            } else {
                assertThatThrownBy(() -> MapWorkspaces.mapWorkspaces(CWD, pkg, manifests(declared), NpmRelease.PORTED))
                  .hasMessage(message);
            }
        } else {
            assertThat(relative(MapWorkspaces.mapWorkspaces(CWD, pkg, manifests(declared), NpmRelease.PORTED)))
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
            assertThatThrownBy(() -> MapWorkspaces.virtual(CWD, lockfile, NpmRelease.PORTED)).hasMessage(c.get("virtualError")
              .get("message").asText());
        } else {
            assertThat(relative(MapWorkspaces.virtual(CWD, lockfile, NpmRelease.PORTED))).isEqualTo(json(c.get("virtual")));
        }
    }

    private static String members(Map<?, String> members) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<?, String> e : members.entrySet()) {
            String relative = Relpath.relpath(CWD, e.getValue());
            out.append(out.length() == 0 ? "" : " ").append(e.getKey()).append('=')
              .append(relative.isEmpty() ? "." : relative);
        }
        return out.toString();
    }

    /**
     * What @npmcli/map-workspaces 3.0.4 (npm 10.5.1) answered for these patterns over this layout, on disk and over
     * a lock of it; where 3.0.6 (npm 10.5.2) answered otherwise, its answer follows in the next two columns.
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', nullValues = "-", value = {
      "[\"packages/*\"]|a=packages/a b=packages/b e=packages/e|a=packages/a b=packages/b|-|-",
      "[\"./packages/*\"]|a=packages/a b=packages/b e=packages/e|``|-|a=packages/a b=packages/b",
      "[\"packages/*\",\"!packages/b\"]|a=packages/a e=packages/e|a=packages/a|-|-",
      "[\"!packages/b\",\"packages/*\"]|b=packages/b a=packages/a e=packages/e|a=packages/a b=packages/b|" +
      "a=packages/a e=packages/e|a=packages/a",
      "[\"packages/*\",\"!packages/b\",\"packages/b\"]|a=packages/a b=packages/b e=packages/e|" +
      "a=packages/a b=packages/b|-|-",
      "[\"packages/**\",\"!packages/c/**\"]|a=packages/a b=packages/b e=packages/e|a=packages/a b=packages/b|-|-",
      "[\"!packages/c/**\",\"packages/**\"]|d=packages/c/d a=packages/a b=packages/b e=packages/e|" +
      "a=packages/a b=packages/b d=packages/c/d|a=packages/a b=packages/b e=packages/e|a=packages/a b=packages/b",
      "[\"packages/**\",\"!packages/c/**\",\"packages/c/d\"]|a=packages/a b=packages/b d=packages/c/d e=packages/e|" +
      "a=packages/a b=packages/b d=packages/c/d|-|-",
      "[\"packages/a\",\"packages/a\"]|a=packages/a|a=packages/a|-|-",
      "[\"/packages/a\",\"tools/*\"]|a=packages/a t=tools/t|a=packages/a t=tools/t|-|-",
      "[\"./packages/a/\",\"./tools/t\"]|a=packages/a t=tools/t|``|-|t=tools/t",
      "[\"packages/*\",\"!!packages/b\",\"!!!packages/a\"]|b=packages/b e=packages/e|b=packages/b|-|-",
      "[\"tools/*\",\"packages/*\"]|t=tools/t a=packages/a b=packages/b e=packages/e|" +
      "a=packages/a b=packages/b t=tools/t|-|t=tools/t a=packages/a b=packages/b",
      "[\"packages/b\",\"packages/a\"]|b=packages/b a=packages/a|a=packages/a b=packages/b|-|" +
      "b=packages/b a=packages/a",
      "[\"!packages/*\"]|``|``|-|-",
      "[\"packages/*/\"]|a=packages/a b=packages/b e=packages/e|``|-|-",
      "[\"**\"]|wsroot=. a=packages/a b=packages/b d=packages/c/d e=packages/e t=tools/t|" +
      "a=packages/a b=packages/b d=packages/c/d t=tools/t|" +
      "a=packages/a b=packages/b d=packages/c/d e=packages/e t=tools/t|" +
      "project=. a=packages/a b=packages/b d=packages/c/d t=tools/t",
      "[\"packages/[ab]\",\"!packages/{b,x}\"]|a=packages/a|a=packages/a|-|-",
      "[\"packages/*\",\"!./packages/b\"]|a=packages/a e=packages/e|a=packages/a b=packages/b|-|a=packages/a",
      "[\"packages/*\",\"!packages/b/\"]|a=packages/a e=packages/e|a=packages/a b=packages/b|-|-",
      "[\".\"]|wsroot=.|``|-|-",
      "[\"./\"]|wsroot=.|``|``|project=.",
      "[\"packages/.h\"]|h=packages/.h|``|-|-",
      "[\"packages/.*\"]|h=packages/.h|``|-|-",
      "{\"packages\":[\"packages/a\"]}|a=packages/a|a=packages/a|-|-"
    })
    void membersBeforeAndFrom3_0_6(String workspaces, String onDisk3_0_4, String virtual3_0_4,
                                   @Nullable String onDisk3_0_6, @Nullable String virtual3_0_6) {
        Map<String, String> manifests = new LinkedHashMap<>();
        manifests.put("", "{\"name\":\"wsroot\",\"version\":\"1.0.0\"}");
        manifests.put("packages/a", "{\"name\":\"a\",\"version\":\"1.0.0\"}");
        manifests.put("packages/b", "{\"name\":\"b\",\"version\":\"1.0.0\"}");
        manifests.put("packages/c/d", "{\"name\":\"d\",\"version\":\"1.0.0\"}");
        manifests.put("tools/t", "{\"name\":\"t\",\"version\":\"1.0.0\"}");
        manifests.put("packages/.h", "{\"name\":\"h\",\"version\":\"1.0.0\"}");
        manifests.put("packages/e", "{\"version\":\"1.0.0\"}");
        manifests.put("packages/a/node_modules/n", "{\"name\":\"n\"}");
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder().manifests(manifests).build();
        Map<String, Object> pkg = obj("workspaces", json(workspaces));
        assertThat(members(MapWorkspaces.mapWorkspaces(CWD, pkg, files, NpmRelease.of("10.5.1"))))
          .isEqualTo(onDisk3_0_4);
        assertThat(members(MapWorkspaces.mapWorkspaces(CWD, pkg, files, NpmRelease.of("10.5.2"))))
          .isEqualTo(onDisk3_0_6 == null ? onDisk3_0_4 : onDisk3_0_6);

        Map<String, Object> packages = obj("", obj("workspaces", json(workspaces)));
        packages.put("packages/a", obj("name", "a"));
        packages.put("packages/b", obj("name", "b"));
        packages.put("packages/c/d", obj("name", "d"));
        packages.put("tools/t", obj());
        packages.put("node_modules/a", obj("link", true));
        packages.put("packages/a/node_modules/n", obj());
        Map<String, Object> lockfile = obj("packages", packages);
        assertThat(members(MapWorkspaces.virtual(CWD, lockfile, NpmRelease.of("10.5.1"))))
          .isEqualTo(virtual3_0_4);
        assertThat(members(MapWorkspaces.virtual(CWD, lockfile, NpmRelease.of("10.5.2"))))
          .isEqualTo(virtual3_0_6 == null ? virtual3_0_4 : virtual3_0_6);
    }

    // what @npmcli/map-workspaces 2.0.0 (npm 8.5.2), 2.0.1 (npm 8.5.3) and 2.0.2 (npm 8.5.4) answered over this
    // layout on disk: before 2.0.2 a name two paths share is refused as soon as the second is found, though a pattern
    // before took the first away; before 2.0.1 a backslash is glob's escape
    @Test
    void duplicateNamesAndBackslashesBefore8_5_4() {
        Map<String, String> manifests = new LinkedHashMap<>();
        manifests.put("", "{\"name\":\"wsroot\"}");
        manifests.put("packages/a", "{\"name\":\"x\"}");
        manifests.put("packages/b", "{\"name\":\"b\"}");
        manifests.put("tools/a", "{\"name\":\"x\"}");
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder().manifests(manifests).build();
        Map<String, Object> negated = obj("workspaces", json("[\"packages/*\",\"!packages/a\",\"tools/*\"]"));
        for (String release : new String[]{"8.5.2", "8.5.3"}) {
            assertThatThrownBy(() -> MapWorkspaces.mapWorkspaces(CWD, negated, files, NpmRelease.of(release)))
              .hasMessage("must not have multiple workspaces with the same name\n" +
                          "package 'x' has conflicts in the following paths:\n" +
                          "    /project/packages/a\n" +
                          "    /project/tools/a");
        }
        assertThat(members(MapWorkspaces.mapWorkspaces(CWD, negated, files, NpmRelease.of("8.5.4"))))
          .isEqualTo("x=tools/a b=packages/b");
        Map<String, Object> backslash = obj("workspaces", json("[\"packages\\\\*\"]"));
        assertThat(members(MapWorkspaces.mapWorkspaces(CWD, backslash, files, NpmRelease.of("8.5.2")))).isEmpty();
        assertThat(members(MapWorkspaces.mapWorkspaces(CWD, backslash, files, NpmRelease.of("8.5.3"))))
          .isEqualTo("x=packages/a b=packages/b");
    }

    private static Object parsed(String text) {
        try {
            return JsonParseEvenBetterErrors.parse(text).getValue();
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return text;
        }
    }
}
