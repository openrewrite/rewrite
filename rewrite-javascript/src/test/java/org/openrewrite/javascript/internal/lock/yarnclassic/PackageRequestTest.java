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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * yarn 1.22.22's __tests__/package-request.js.
 */
class PackageRequestTest {

    @Test
    void producesValidRemoteTypeForAGitOverSshDep() {
        PackageRequest request = prepareRequest("private-dep@github:yarnpkg/private-dep#1.0.0", "1.0.0",
                "git+ssh://git@github.com/yarnpkg/private-dep.git#d6c57894210c52be02da7859dbb5205feb85d8b0", null);
        assertThat(remoteType(request, "git")).isEqualTo("git");
        assertThat(remoteType(request, "tarball")).isEqualTo("git");
    }

    @Test
    void producesValidRemoteTypeForAGitOverHttpsDep() {
        PackageRequest request = prepareRequest("public-dep@yarnpkg/public-dep#1.0.0", "1.0.0",
                "git+https://github.com/yarnpkg/public-dep#1fde368", null);
        assertThat(remoteType(request, "git")).isEqualTo("git");
        assertThat(remoteType(request, "tarball")).isEqualTo("git");
    }

    @Test
    void producesValidRemoteTypeForAGitPublicDep() {
        PackageRequest request = prepareRequest("public-dep@yarnpkg/public-dep#1fde368", "1.0.0",
                "https://codeload.github.com/yarnpkg/public-dep/tar.gz/1fde368", null);
        assertThat(remoteType(request, "git")).isEqualTo("git");
        assertThat(remoteType(request, "tarball")).isEqualTo("tarball");
    }

    @Test
    void parentNamesFlowInTheRequest() {
        PackageRequest parentRequest = prepareRequest("parent@1.0.0", "1.0.0",
                "git+ssh://git@github.com/yarnpkg/parent.git", null);
        PackageRequest childRequest = prepareRequest("child@1.0.0", "1.0.0",
                "git+ssh://git@github.com/yarnpkg/child.git", parentRequest);
        assertThat(childRequest.parentNames).contains(parentRequest.pattern);
    }

    private static String remoteType(PackageRequest request, String remoteType) {
        return String.valueOf(requireNonNull(requireNonNull(request.getLocked(remoteType)).remote).type);
    }

    private static PackageRequest prepareRequest(String pattern, String version, String resolved,
                                                 @Nullable PackageRequest parentRequest) {
        Map<String, @Nullable Object> privateDepCache = new LinkedHashMap<>();
        privateDepCache.put(pattern, entry(version, resolved));
        List<String> parentNames = new ArrayList<>();
        if (parentRequest != null) {
            parentNames.addAll(parentRequest.parentNames);
            parentNames.add(parentRequest.pattern);
            Manifest lock = requireNonNull(parentRequest.getLocked("tarball"));
            privateDepCache.put(parentRequest.pattern, entry(String.valueOf(lock.getVersion()),
                    requireNonNull(lock.remote).resolved));
        }
        Lockfile lockfile = new Lockfile(privateDepCache, null, null);
        PackageResolver resolver = YarnClassicScenarios.resolver(YarnClassicScenarios.defaults(), lockfile);
        return new PackageRequest(DependencyRequestPattern.builder()
                .pattern(pattern)
                .registry("npm")
                .optional(false)
                .parentNames(parentNames)
                .parentRequest(parentRequest)
                .build(), resolver);
    }

    private static Map<String, @Nullable Object> entry(String version, @Nullable String resolved) {
        Map<String, @Nullable Object> entry = new LinkedHashMap<>();
        entry.put("version", version);
        entry.put("resolved", resolved);
        return entry;
    }
}
