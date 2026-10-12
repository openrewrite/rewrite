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
package org.openrewrite.javascript.internal.lock.bun;

/**
 * Port of bun's src/install/dependency.zig {@code Dependency.Behavior} (bun 1.3.10): the bits of a
 * {@code packed struct(u8)}.
 */
final class Behavior {

    static final int PROD = 1 << 1;
    static final int OPTIONAL = 1 << 2;
    static final int DEV = 1 << 3;
    static final int PEER = 1 << 4;
    static final int WORKSPACE = 1 << 5;
    static final int BUNDLED = 1 << 6;

    static final Behavior NONE = new Behavior(0);

    final int bits;

    private Behavior(int bits) {
        this.bits = bits;
    }

    static Behavior of(int bits) {
        return new Behavior(bits);
    }

    boolean isProd() {
        return (bits & PROD) != 0;
    }

    boolean isOptional() {
        return (bits & OPTIONAL) != 0 && (bits & PEER) == 0;
    }

    boolean isOptionalPeer() {
        return (bits & OPTIONAL) != 0 && (bits & PEER) != 0;
    }

    boolean optional() {
        return (bits & OPTIONAL) != 0;
    }

    boolean isDev() {
        return (bits & DEV) != 0;
    }

    boolean isPeer() {
        return (bits & PEER) != 0;
    }

    boolean isWorkspace() {
        return (bits & WORKSPACE) != 0;
    }

    /**
     * {@code isWorkspaceOnly} (before 1.2.19): a workspace and nothing else, where a member's own dependencies also
     * carry the workspace bit.
     */
    boolean isWorkspaceOnly() {
        return isWorkspace() && (bits & (DEV | PROD | OPTIONAL | PEER)) == 0;
    }

    /**
     * The release's workspace test: {@link #isWorkspace()}, or {@link #isWorkspaceOnly()} before 1.2.19.
     */
    boolean isWorkspace(BunRelease release) {
        return release.tellsDependencyGroupsApart() ? isWorkspace() : isWorkspaceOnly();
    }

    boolean isBundled() {
        return (bits & BUNDLED) != 0;
    }

    boolean eq(Behavior rhs) {
        return bits == rhs.bits;
    }

    boolean includes(Behavior rhs) {
        return (bits & rhs.bits) != 0;
    }

    Behavior add(int bit) {
        return new Behavior(bits | bit);
    }

    Behavior set(int bit, boolean value) {
        return new Behavior(value ? bits | bit : bits & ~bit);
    }

    boolean isRequired() {
        return !isOptional();
    }

    /**
     * {@code cmp} as the release orders behaviors: from 1.2.19 a workspace, then dev, optional, prod and peer
     * dependencies first ({@link #cmp(Behavior)}); before, each of prod, dev, optional, peer and workspace last, in
     * that order of precedence.
     */
    int cmp(Behavior rhs, BunRelease release) {
        if (release.tellsDependencyGroupsApart() || eq(rhs)) {
            return cmp(rhs);
        }
        if (isProd() != rhs.isProd()) {
            return isProd() ? 1 : -1;
        }
        if (isDev() != rhs.isDev()) {
            return isDev() ? 1 : -1;
        }
        if (isOptional() != rhs.isOptional()) {
            return isOptional() ? 1 : -1;
        }
        if (isPeer() != rhs.isPeer()) {
            return isPeer() ? 1 : -1;
        }
        if (isWorkspace() != rhs.isWorkspace()) {
            return isWorkspace() ? 1 : -1;
        }
        return 0;
    }

    int cmp(Behavior rhs) {
        if (eq(rhs)) {
            return 0;
        }
        if (isWorkspace() != rhs.isWorkspace()) {
            return isWorkspace() ? -1 : 1;
        }
        if (isDev() != rhs.isDev()) {
            return isDev() ? -1 : 1;
        }
        if (isOptional() != rhs.isOptional()) {
            return isOptional() ? -1 : 1;
        }
        if (isProd() != rhs.isProd()) {
            return isProd() ? -1 : 1;
        }
        if (isPeer() != rhs.isPeer()) {
            return isPeer() ? -1 : 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Behavior && ((Behavior) o).bits == bits;
    }

    @Override
    public int hashCode() {
        return bits;
    }
}
