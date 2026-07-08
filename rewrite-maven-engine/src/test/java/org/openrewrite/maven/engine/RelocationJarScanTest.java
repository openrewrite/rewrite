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
package org.openrewrite.maven.engine;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The published (shaded) artifact is held to an <em>empty allowlist</em>: every class — including multi-release
 * overlays under {@code META-INF/versions/} — must live under {@code org/openrewrite/}. Anything else (unrelocated
 * aether/maven/plexus, but equally gson, asm, slf4j, or a future transitive) fails the build, so a dependency bump
 * can never silently reintroduce the classpath-collision hazard shading exists to prevent. The excluded Apache http
 * transport must be absent even in relocated form, and {@code META-INF/sisu} must be stripped.
 */
class RelocationJarScanTest {

    // Excluded from the shade entirely; must not appear even relocated (HttpSender is the sole transport).
    private static final List<String> FORBIDDEN_ANYWHERE = Arrays.asList(
            "transport/apache/",       // maven-resolver-transport-apache (relocated or not)
            "org/apache/http/",        // httpclient / httpcore
            "org/apache/commons/codec/");

    // Logging binds host-side; slf4j must resolve from the POM, never from inside the jar (relocated or not).
    private static final String SLF4J = "org/slf4j/";

    private static final String SHADED_PREFIX = "org/openrewrite/maven/engine/shaded/";
    private static final String MR_PREFIX = "META-INF/versions/";

    @Test
    void everyClassInJarIsUnderOrgOpenrewrite() throws Exception {
        File jar = shadowJar();

        List<String> outsideAllowlist = new ArrayList<>();
        List<String> excludedLeaks = new ArrayList<>();
        boolean sawRelocatedAether = false;
        boolean sawEngineClass = false;
        boolean sawSisu = false;
        int classes = 0;

        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("META-INF/sisu/")) {
                    sawSisu = true;
                }
                if (!name.endsWith(".class")) {
                    continue;
                }
                classes++;

                // Multi-release overlays are classes too; strip META-INF/versions/<n>/ before the allowlist check.
                String path = name;
                if (path.startsWith(MR_PREFIX)) {
                    int slash = path.indexOf('/', MR_PREFIX.length());
                    path = slash < 0 ? path : path.substring(slash + 1);
                }

                // The empty allowlist: org/openrewrite/** only (module-info is excluded by the build outright).
                if (!path.startsWith("org/openrewrite/")) {
                    outsideAllowlist.add(name);
                }
                if (path.startsWith(SHADED_PREFIX + "org/eclipse/aether/")) {
                    sawRelocatedAether = true;
                }
                if (path.equals("org/openrewrite/maven/engine/MavenEngine.class")) {
                    sawEngineClass = true;
                }
                for (String forbidden : FORBIDDEN_ANYWHERE) {
                    if (path.contains(forbidden)) {
                        excludedLeaks.add(name);
                    }
                }
                if (path.contains(SLF4J)) {
                    excludedLeaks.add(name);
                }
            }
        }

        assertTrue(classes > 100, "expected the whole resolver stack bundled, only saw " + classes + " classes");
        assertTrue(sawRelocatedAether, "expected relocated org.eclipse.aether classes under " + SHADED_PREFIX);
        assertTrue(sawEngineClass, "expected the engine's own MavenEngine.class in the jar");
        assertTrue(outsideAllowlist.isEmpty(), "classes outside the org/openrewrite/** allowlist leaked:\n  "
                + String.join("\n  ", outsideAllowlist));
        assertTrue(excludedLeaks.isEmpty(), "excluded dependencies (http transport, slf4j) leaked into the jar:\n  "
                + String.join("\n  ", excludedLeaks));
        assertFalse(sawSisu, "META-INF/sisu/** must be stripped (no DI container is used)");
    }

    static File shadowJar() {
        String path = System.getProperty("engine.shadowJar");
        assertNotNull(path, "engine.shadowJar system property must be set by the build");
        File jar = new File(path);
        assertTrue(jar.isFile(), "shaded jar not found at " + path);
        return jar;
    }
}
