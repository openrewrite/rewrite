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
package org.openrewrite.maven.parity.corpus;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * All materialized state lives under gitignored {@code .corpus/} in the working directory;
 * only {@code corpus.yaml} is committed.
 */
public final class CorpusPaths {
    private CorpusPaths() {
    }

    public static Path manifest() {
        return Paths.get("corpus.yaml");
    }

    public static Path corpus() {
        return Paths.get(".corpus");
    }

    public static Path store() {
        return corpus().resolve("store");
    }

    public static Path poms() {
        return corpus().resolve("poms");
    }

    public static Path reactors() {
        return corpus().resolve("reactors");
    }

    public static Path groundTruth() {
        return corpus().resolve("ground-truth");
    }

    public static Path snapshots() {
        return corpus().resolve("snapshots");
    }

    public static Path stubs() {
        return corpus().resolve("stubs");
    }

    public static Path tools() {
        return corpus().resolve("tools");
    }

    public static Path localRepository() {
        return corpus().resolve("m2");
    }
}
