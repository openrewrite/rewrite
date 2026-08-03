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
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Reads the resolved dependencies of an already-installed environment (a virtual env): one
 * {@link ResolvedDependency} per {@code site-packages/*.dist-info}. The dist-info metadata is
 * equivalent to {@code pip freeze} output without running a subprocess.
 */
public final class InstalledEnvParser {

    private InstalledEnvParser() {
    }

    public static List<ResolvedDependency> parse(Path env) {
        Path sitePackages = sitePackages(env);
        if (sitePackages == null) {
            return emptyList();
        }
        List<ResolvedDependency> resolved = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(sitePackages, "*.dist-info")) {
            for (Path distInfo : ds) {
                String dir = distInfo.getFileName().toString();
                String base = dir.substring(0, dir.length() - ".dist-info".length());
                int dash = base.lastIndexOf('-');
                if (dash > 0) {
                    resolved.add(new ResolvedDependency(base.substring(0, dash), base.substring(dash + 1), null, null));
                }
            }
        } catch (IOException e) {
            return emptyList();
        }
        resolved.sort(Comparator.comparing(ResolvedDependency::getName));
        return resolved;
    }

    /** {@code <env>/lib/pythonX.Y/site-packages} (POSIX) or {@code <env>/Lib/site-packages} (Windows). */
    private static @Nullable Path sitePackages(Path env) {
        Path lib = env.resolve("lib");
        if (Files.isDirectory(lib)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(lib, "python*")) {
                for (Path p : ds) {
                    Path sp = p.resolve("site-packages");
                    if (Files.isDirectory(sp)) {
                        return sp;
                    }
                }
            } catch (IOException ignored) {
            }
        }
        Path winSp = env.resolve("Lib").resolve("site-packages");
        return Files.isDirectory(winSp) ? winSp : null;
    }
}
