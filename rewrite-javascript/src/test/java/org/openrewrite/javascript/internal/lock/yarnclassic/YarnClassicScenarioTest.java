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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Expect;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockScenario.Phase;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replays every recorded edit {@link YarnClassicScenarios} finds through {@link YarnClassicLockRegenerator} and asserts
 * its {@code expect} line; {@code REWRITE_LOCK_YARN_CLASSIC_SCENARIO_RESULTS} names a file for one line per edit.
 */
class YarnClassicScenarioTest extends LockScenarioTest {

    private static final Pattern SYNTAX_ERROR_HEAD = Pattern.compile("^SyntaxError: (\\.\\./)*package\\.json:");

    YarnClassicScenarioTest() {
        super("REWRITE_LOCK_YARN_CLASSIC_SCENARIO_RESULTS", "yarn");
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (YarnClassicScenarios.Scenario scenario : YarnClassicScenarios.discover()) {
            if (scenario.isEdit()) {
                out.add(Arguments.of(scenario.getLabel(), scenario));
            }
        }
        return out.stream();
    }

    /**
     * The registry as the scenario recorded it ({@code http/<name>}, {@code http/<name>-<version>.tgz}, a 404 where its
     * {@code registry-config} said); a downloaded fixture's empty versions come from its {@code <name>-<version>}.
     */
    static final class ScenarioRegistry extends RecordedRegistry<YarnClassicScenarios.Scenario>
            implements RegistrySource {
        ScenarioRegistry(YarnClassicScenarios.Scenario scenario) {
            super(scenario);
        }

        @Override
        public @Nullable String packument(String registry, String name, boolean full, @Nullable String authorization) {
            String mirror = mirror(registry, "npmjs", "yarnpkg");
            String body = mirror == null ? scenario.http(name) : scenario.file(mirror + "/" + name);
            if (body == null) {
                notRecorded(name);
                return null;
            }
            return withRecordedVersions(name, body);
        }

        @Override
        public byte @Nullable [] tarball(String url, String name, String version, @Nullable String authorization) {
            if (scenario.notFound(url)) {
                return null;
            }
            String file = name + "-" + version + ".tgz";
            String mirror = mirror(url, "npmjs", "yarnpkg");
            byte[] tgz = mirror == null ? scenario.tarball(file) : scenario.bytes(mirror + "/" + file);
            if (tgz == null) {
                notRecorded(file + " " + url);
            }
            return tgz;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("edits")
    void regeneratesWhatYarnWrote(String name, YarnClassicScenarios.Scenario scenario) {
        Expect expect = Expect.of(scenario.expect());
        skipUnlessReplayed(scenario.getLabel(), scenario, expect);
        ScenarioRegistry http = new ScenarioRegistry(scenario);
        Result result = new YarnClassicLockRegenerator(scenario.inputs(Phase.AFTER,
                scenario.file("before")), http).regenerate();
        check(scenario.getLabel(), expect, scenario.file("after"), result, http);
    }

    /**
     * yarn-enable-lockfile-versions writes the Node version into the lock's header, which the engine cannot know from
     * a host that does not say, unless a lockfile flag keeps yarn from writing the lock at all.
     */
    @ParameterizedTest
    @CsvSource({
            "small-lockfile-versions-header, the Node version yarn-enable-lockfile-versions writes",
            "open-opt-pure-lockfile-lockfile-versions,"
    })
    void readsTheNodeVersionOnlyWhereYarnWritesTheLock(String name, @Nullable String cannotKnow) {
        YarnClassicScenarios.Scenario scenario = YarnClassicScenarios.discover().stream()
                .filter(s -> s.getName().equals(name)).findFirst().orElseThrow(IllegalStateException::new);
        Host withoutNode = scenario.host().toBuilder().nodeVersion(null).build();
        YarnClassicLockRegenerator.Inputs inputs = scenario.inputs(Phase.AFTER,
                scenario.file("before")).toBuilder().host(withoutNode).build();
        Result result = new YarnClassicLockRegenerator(inputs, new ScenarioRegistry(scenario)).regenerate();
        if (cannotKnow == null) {
            assertThat(result.getLockFileContent()).isEqualTo(scenario.file("after"));
        } else {
            assertThat(requireNonNull(result.getFailure()).getDetail()).isEqualTo("the engine cannot know " +
                                                                                  cannotKnow);
        }
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        String rest = expect.getRest();
        String detail = failure.getDetail();
        switch (expect.getKind()) {
            case "cannot-know":
                return failure.getKind() == Kind.CANNOT_KNOW && namesEvery(detail, rest) && http.missing.isEmpty();
            case "malformed":
                return failure.getReason() == Reason.MALFORMED_LOCK || failure.getReason() == Reason.MALFORMED_MANIFEST;
            case "pm-rejects":
                // a package the registry does not have is the one route that is not recorded
                return failure.getKind() == Kind.PM_REJECTS && saysWhatYarnSaid(detail, rest) &&
                       (http.missing.isEmpty() || failure.getReason() == Reason.PACKAGE_NOT_FOUND &&
                        (rest.startsWith("Couldn't find package") || rest.endsWith(": Not found")));
            case "pm-fails":
                return failure.getKind() == Kind.PM_FAILS && saysWhatYarnSaid(detail, rest) && http.missing.isEmpty();
            case "second-install-rejects":
            case "unstable":
                return failure.getKind() == Kind.SECOND_INSTALL;
            default:
                return false;
        }
    }

    // the head of yarn's error line; V8's JSON.parse wording is not ported, only the error and the file it names
    private static boolean saysWhatYarnSaid(String detail, String message) {
        String head = message.endsWith(".") ? message.substring(0, message.length() - 1) : message;
        Matcher syntaxError = SYNTAX_ERROR_HEAD.matcher(head);
        if (syntaxError.find()) {
            return detail.contains(syntaxError.group());
        }
        return detail.contains(head);
    }

    // every package pattern, spec or url the expect line names
    private static boolean namesEvery(String detail, String what) {
        for (String token : what.split("\\s+")) {
            String t = token.replaceAll("^[(]+", "").replaceAll("[),;:]+$", "");
            if ((t.indexOf('@') > 0 || t.contains("://") || t.matches("^[a-z]+:[^/].*")) && !detail.contains(t)) {
                return false;
            }
        }
        return true;
    }
}
