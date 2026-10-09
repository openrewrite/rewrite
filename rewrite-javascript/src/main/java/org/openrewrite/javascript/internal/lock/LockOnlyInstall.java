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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A package manager's lock-only install as a port runs it: its failures, worded with the command's name, and the
 * second install over the lock written, which must change nothing.
 */
public final class LockOnlyInstall {

    public static final LockOnlyInstall NPM = new LockOnlyInstall("npm");
    public static final LockOnlyInstall BUN = new LockOnlyInstall("bun");
    public static final LockOnlyInstall YARN = new LockOnlyInstall("yarn");
    public static final LockOnlyInstall PNPM = new LockOnlyInstall("pnpm");

    private final String command;

    private LockOnlyInstall(String command) {
        this.command = command;
    }

    /**
     * @param code the package manager's error code, when it words its errors by one
     */
    public Failure failure(Kind kind, Reason reason, @Nullable String packageName, @Nullable String code,
                           String message) {
        String lead = kind == Kind.CANNOT_KNOW ? "the engine cannot know " :
                describe(kind) + (code == null ? "" : " (" + code + ")") + ": ";
        return new Failure(kind, reason, packageName, lead + message);
    }

    public Failure failure(CannotKnow e) {
        return failure(Kind.CANNOT_KNOW, e.getReason(), null, null, e.getSubject());
    }

    public Failure failure(Rejected e) {
        return failure(e.getKind(), e.getReason(), e.getPackageName(), null, String.valueOf(e.getMessage()));
    }

    public Failure rejects(Reason reason, @Nullable String packageName, String message) {
        return failure(Kind.PM_REJECTS, reason, packageName, null, message);
    }

    public Failure fails(Reason reason, @Nullable String packageName, String message) {
        return failure(Kind.PM_FAILS, reason, packageName, null, message);
    }

    /**
     * The written lock, once the second install over it changes nothing; else the failure.
     */
    public Result verify(String written, UnaryOperator<String> install,
                         Function<RuntimeException, Failure> failureOf) {
        String rewritten;
        try {
            rewritten = install.apply(written);
        } catch (NodeRegistryException e) {
            throw e;
        } catch (RuntimeException e) {
            Failure failure = failureOf.apply(e);
            return Result.failure(new Failure(Kind.SECOND_INSTALL, failure.getReason(), failure.getPackageName(),
                    describe(Kind.SECOND_INSTALL) + (failure.getKind() == Kind.PM_FAILS ? " would fail: " :
                            " would reject it: ") + failure.getDetail()));
        }
        if (!rewritten.equals(written)) {
            return Result.failure(new Failure(Kind.SECOND_INSTALL, Reason.RESOLUTION_REQUIRED, null,
                    describe(Kind.SECOND_INSTALL) + " would rewrite it: " + firstDifference(written, rewritten)));
        }
        return Result.success(written);
    }

    private String describe(Kind kind) {
        switch (kind) {
            case PM_REJECTS:
                return command + " rejects this edit";
            case PM_FAILS:
                return command + " fails on this edit";
            case DISCARDS_LOCK:
                return command + " would discard the lock and resolve everything from scratch";
            case DELETES_LOCK:
                return command + " deletes the lock";
            case SECOND_INSTALL:
            default:
                return "a second " + command + " install over the lock written for this edit";
        }
    }

    private static String firstDifference(String a, String b) {
        String[] as = a.split("\n", -1);
        String[] bs = b.split("\n", -1);
        for (int i = 0; i < Math.max(as.length, bs.length); i++) {
            String x = i < as.length ? as[i] : "";
            String y = i < bs.length ? bs[i] : "";
            if (!x.equals(y)) {
                return "line " + (i + 1) + " \"" + x.trim() + "\" becomes \"" + y.trim() + "\"";
            }
        }
        return "the files differ";
    }
}
