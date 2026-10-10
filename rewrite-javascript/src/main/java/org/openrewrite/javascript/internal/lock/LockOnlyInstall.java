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

import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.NodeRegistryException;

import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A package manager's lock-only install as a port runs it: its failures, each of the kind {@link #kindOf} names and
 * worded with the command's name, and the second install over the lock written, which must change nothing.
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
     * The one rule for a failure's kind: what the engine cannot see, the package manager's own verdict, an error with
     * no code of its own, or what it does to the lock instead of installing over it.
     */
    public static Kind kindOf(RuntimeException outcome) {
        if (outcome instanceof CannotKnow) {
            return Kind.CANNOT_KNOW;
        }
        if (outcome instanceof NodeRegistryException) {
            NodeRegistryException.Reason reason = ((NodeRegistryException) outcome).getReason();
            return reason == NodeRegistryException.Reason.PACKAGE_NOT_FOUND ||
                   reason == NodeRegistryException.Reason.VERSION_NOT_FOUND ? Kind.PM_REJECTS : Kind.CANNOT_KNOW;
        }
        if (outcome instanceof Rejected) {
            return ((Rejected) outcome).getOutcome() == Rejected.Outcome.CRASH ? Kind.PM_FAILS : Kind.PM_REJECTS;
        }
        throw outcome;
    }

    /**
     * A walk from {@code runsIn}, a directory below the lock's, that installs {@code installRoot} directories above it
     * writes the lock there; elsewhere than at this {@code lock} is what the engine cannot know.
     */
    public void requireInstallsAt(int installRoot, String runsIn, String lock) {
        int lockDir = runsIn.isEmpty() ? 0 : runsIn.split("/").length;
        if (installRoot > lockDir) {
            StringBuilder up = new StringBuilder("..");
            for (int i = installRoot - lockDir; i > 1; i--) {
                up.append("/..");
            }
            throw new CannotKnow("the lock " + command + " writes: it installs at the workspace root " + up +
                                 ", whose workspaces list this project, and writes that root's " + lock);
        }
        if (installRoot < lockDir) {
            throw new CannotKnow("the lock " + command + " writes: run in " + runsIn + " it installs " + installRoot +
                                 " directories above it, not at this " + lock);
        }
    }

    /**
     * @param outcome a {@link CannotKnow} or a {@link Rejected}; anything else is rethrown
     */
    public Failure failure(RuntimeException outcome) {
        if (outcome instanceof CannotKnow) {
            return cannotKnow((CannotKnow) outcome);
        }
        if (!(outcome instanceof Rejected)) {
            throw outcome;
        }
        Rejected e = (Rejected) outcome;
        Kind kind = kindOf(e);
        String what = e.getOutcome() == Rejected.Outcome.DISCARDS_LOCK ?
                command + " would discard the lock and resolve everything from scratch" :
                e.getOutcome() == Rejected.Outcome.DELETES_LOCK ? command + " deletes the lock" : describe(kind);
        return new Failure(kind, e.getReason(), e.getPackageName(), what +
                (e.getCode() == null ? "" : " (" + e.getCode() + ")") + ": " + e.getMessage());
    }

    /**
     * What the engine cannot know, worded alike for every package manager.
     */
    public static Failure cannotKnow(CannotKnow e) {
        return new Failure(Kind.CANNOT_KNOW, e.getReason(), null, "the engine cannot know " + e.getSubject());
    }

    /**
     * The registry's answer to a document the package manager asked for, the engine as its client.
     */
    public static Failure failure(NodeRegistryException e) {
        Reason reason;
        switch (e.getReason()) {
            case AUTH_FAILED:
                reason = Reason.AUTH_FAILED;
                break;
            case PACKAGE_NOT_FOUND:
                reason = SharedError.PACKAGE_NOT_FOUND.getReason();
                break;
            case VERSION_NOT_FOUND:
                reason = SharedError.NO_MATCHING_VERSION.getReason();
                break;
            case UNREACHABLE:
            default:
                reason = Reason.REGISTRY_UNREACHABLE;
                break;
        }
        return new Failure(kindOf(e), reason, e.getPackageName(), String.valueOf(e.getMessage()));
    }

    /**
     * The written lock, once the second install over it changes nothing; else the failure. What the engine cannot
     * know there stays cannot-know.
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
            if (failure.getKind() == Kind.CANNOT_KNOW) {
                return Result.failure(new Failure(Kind.CANNOT_KNOW, failure.getReason(), failure.getPackageName(),
                        failure.getDetail() + " (in " + describe(Kind.SECOND_INSTALL) + ")"));
            }
            return Result.failure(new Failure(Kind.SECOND_INSTALL, failure.getReason(), failure.getPackageName(),
                    describe(Kind.SECOND_INSTALL) + (failure.getKind() == Kind.PM_FAILS ? " would fail: " :
                            " would reject it: ") + failure.getDetail(), written));
        }
        if (!rewritten.equals(written)) {
            return Result.failure(new Failure(Kind.SECOND_INSTALL, Reason.RESOLUTION_REQUIRED, null,
                    describe(Kind.SECOND_INSTALL) + " would rewrite it: " + firstDifference(written, rewritten),
                    written));
        }
        return Result.success(written);
    }

    private String describe(Kind kind) {
        switch (kind) {
            case PM_REJECTS:
                return command + " rejects this edit";
            case PM_FAILS:
                return command + " fails on this edit";
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
