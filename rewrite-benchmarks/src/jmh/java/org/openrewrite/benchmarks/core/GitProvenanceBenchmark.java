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
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openrewrite.jgit.lib.RepositoryBuilder;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.GitProvenance.CommitHistory;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Measures the cost of {@link GitProvenance#fromProjectDirectory} as the commit-history walk is bounded
 * by {@link CommitHistory}. Runs against this repository's own git working tree, which has a deep history
 * and many committers, so the cost curve from {@code none()} (no walk) to {@code full()} (the entire
 * history) is realistic.
 * <p>
 * Run with: {@code ./gradlew :rewrite-benchmarks:jmh -Pjmh.includes=GitProvenance}
 * <p>
 * The repository is auto-detected by walking up from {@code user.dir}; override with
 * {@code -Drewrite.repoRoot=/path/to/a/deep/clone} (e.g. a full clone of openrewrite/rewrite).
 */
@Fork(1)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class GitProvenanceBenchmark {

    Path projectDir;

    @Setup(Level.Trial)
    public void setup() {
        String override = System.getProperty("rewrite.repoRoot");
        File start = override != null && !override.isEmpty() ?
                new File(override) :
                new File(System.getProperty("user.dir"));
        File gitDir = new RepositoryBuilder().findGitDir(start).getGitDir();
        if (gitDir == null || !gitDir.exists()) {
            throw new IllegalStateException("No .git found from " + start.getAbsolutePath() +
                    "; set -Drewrite.repoRoot to a git working tree with deep history.");
        }
        // fromProjectDirectory re-resolves the git dir from this starting directory.
        projectDir = start.toPath();

        // Fail loud rather than silently measuring nothing: a shallow clone has no deep history to walk,
        // and the whole point of this benchmark is a deep history. (Linked git worktrees are supported.)
        GitProvenance full = GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.full());
        if (full == null || full.getCommitters() == null || full.getCommitters().isEmpty()) {
            throw new IllegalStateException("GitProvenance produced no committers for " + start.getAbsolutePath() +
                    " (a shallow clone has no history to walk). Set -Drewrite.repoRoot to a clone with full " +
                    "history, e.g. a clone of openrewrite/rewrite.");
        }
    }

    @Benchmark
    public void none(Blackhole bh) {
        bh.consume(GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.none()));
    }

    @Benchmark
    public void full(Blackhole bh) {
        bh.consume(GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.full()));
    }

    @Benchmark
    public void lastCommits100(Blackhole bh) {
        bh.consume(GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.lastCommits(100)));
    }

    @Benchmark
    public void lastCommits1000(Blackhole bh) {
        bh.consume(GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.lastCommits(1000)));
    }

    @Benchmark
    public void sinceDaysAgo90(Blackhole bh) {
        bh.consume(GitProvenance.fromProjectDirectory(projectDir, null, null, CommitHistory.sinceDaysAgo(90)));
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(GitProvenanceBenchmark.class.getSimpleName())
                .addProfiler(GCProfiler.class)
                .build();
        new Runner(opt).run();
    }
}
