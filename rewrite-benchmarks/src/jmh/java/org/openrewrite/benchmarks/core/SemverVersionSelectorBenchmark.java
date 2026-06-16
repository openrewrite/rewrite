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
package org.openrewrite.benchmarks.core;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Measures the cost of evaluating a node-semver version selector against the full published version
 * list of a dependency, as happens in {@code UpgradeDependencyVersion}/{@code ChangeDependency} and
 * friends. The hot path allocates a {@link java.util.regex.Matcher} (and its regex-group array) per
 * candidate version; this benchmark exposes that allocation via {@code -prof gc} ({@code gc.alloc.rate}).
 * <p>
 * The version list mimics the shape of {@code org.springframework.boot:spring-boot}: early releases
 * carry a {@code .RELEASE}/{@code .M#}/{@code .RC#}/{@code .BUILD-SNAPSHOT} qualifier, later releases
 * are plain semver with {@code -M#}/{@code -RC#}/{@code -SNAPSHOT} pre-releases.
 */
@Fork(1)
@Measurement(iterations = 5, time = 2)
@Warmup(iterations = 3, time = 2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class SemverVersionSelectorBenchmark {

    private List<String> versions;
    private VersionComparator latestRelease;
    private VersionComparator xRange;
    private VersionComparator hyphenRange;

    @Setup(Level.Trial)
    public void setup() {
        this.versions = springBootStyleVersions();
        this.latestRelease = new LatestRelease(null);
        this.xRange = Objects.requireNonNull(Semver.validate("3.x", null).getValue());
        this.hyphenRange = Objects.requireNonNull(Semver.validate("2.0-3.0", null).getValue());
    }

    @Benchmark
    public void upgradeLatestRelease(Blackhole bh) {
        bh.consume(latestRelease.upgrade("3.0.0", versions));
    }

    @Benchmark
    public void upgradeXRange(Blackhole bh) {
        bh.consume(xRange.upgrade("3.0.0", versions));
    }

    @Benchmark
    public void upgradeHyphenRange(Blackhole bh) {
        bh.consume(hyphenRange.upgrade("3.0.0", versions));
    }

    /**
     * Builds a deterministic version list resembling the published versions of
     * {@code org.springframework.boot:spring-boot} (~280 entries spanning the {@code .RELEASE}-suffixed
     * and plain-semver eras, with milestone/RC/snapshot pre-releases mixed in).
     */
    static List<String> springBootStyleVersions() {
        List<String> versions = new ArrayList<>();
        // 1.x and 2.0-2.2: ".RELEASE" suffix era
        for (int minor = 0; minor <= 5; minor++) {
            for (int patch = 0; patch <= 18; patch++) {
                versions.add("1." + minor + "." + patch + ".RELEASE");
            }
            versions.add("1." + minor + ".0.M1");
            versions.add("1." + minor + ".0.RC1");
            versions.add("1." + minor + ".0.BUILD-SNAPSHOT");
        }
        for (int minor = 0; minor <= 2; minor++) {
            for (int patch = 0; patch <= 11; patch++) {
                versions.add("2." + minor + "." + patch + ".RELEASE");
            }
            versions.add("2." + minor + ".0.M1");
            versions.add("2." + minor + ".0.RC1");
        }
        // 2.3-2.7 and 3.x: plain-semver era
        for (int minor = 3; minor <= 7; minor++) {
            for (int patch = 0; patch <= 18; patch++) {
                versions.add("2." + minor + "." + patch);
            }
            versions.add("2." + minor + ".0-M1");
            versions.add("2." + minor + ".0-RC1");
            versions.add("2." + minor + ".0-SNAPSHOT");
        }
        for (int minor = 0; minor <= 3; minor++) {
            for (int patch = 0; patch <= 13; patch++) {
                versions.add("3." + minor + "." + patch);
            }
            versions.add("3." + minor + ".0-M1");
            versions.add("3." + minor + ".0-RC1");
            versions.add("3." + minor + ".0-SNAPSHOT");
        }
        return versions;
    }

    /**
     * Run from the command line:
     * <pre>./gradlew :rewrite-benchmarks:jmh -Pjmh.includes=SemverVersionSelectorBenchmark</pre>
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SemverVersionSelectorBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
