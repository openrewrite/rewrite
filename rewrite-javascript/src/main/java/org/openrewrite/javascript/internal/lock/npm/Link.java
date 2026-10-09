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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.LinkedHashMap;
import java.util.Objects;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of @npmcli/arborist lib/link.js (npm 11.17.0): a symlink to a node elsewhere, such as a
 * workspace or a {@code file:} directory dependency.
 */
public class Link extends Node {

    public Link(Options options) {
        super(linkOptions(options));
        Node target = options.getTarget();
        if (target != null) {
            setTarget(target);
        } else if (Objects.equals(getRealpath(), getRoot().getPath())) {
            setTarget(getRoot());
        } else {
            setTarget(new Node(options.toBuilder()
                    .path(options.getRealpath())
                    .parent(null)
                    .fsParent(null)
                    .root(getRoot())
                    .build()));
        }
    }

    private static Options linkOptions(Options options) {
        String realpath = options.getRealpath();
        Node target = options.getTarget();
        if (!truthy(realpath) && (target == null || target.getPath() == null)) {
            throw new IllegalArgumentException("must provide realpath for Link node");
        }
        Node root = options.getRoot() != null ? options.getRoot() :
                options.getParent() != null ? options.getParent().getRoot() :
                        options.getFsParent() != null ? options.getFsParent().getRoot() :
                                target != null ? target.getRoot() : null;
        return options.toBuilder()
                .realpath(truthy(realpath) ? realpath : target.getPath())
                .root(root)
                .build();
    }

    @Override
    public Object getVersion() {
        if (target != null) {
            return target.getVersion();
        }
        Object version = pkg.get("version");
        return truthy(version) ? version : "";
    }

    @Override
    public @Nullable Node getTarget() {
        return target;
    }

    public void setTarget(@Nullable Node target) {
        Node current = this.target;
        if (target == current) {
            return;
        }

        if (target == null) {
            current.getLinksIn().remove(this);
            if (getPath() != null) {
                delistFromMeta();
                this.target = null;
                setPackage(new LinkedHashMap<>());
                refreshLocation();
            } else {
                this.target = null;
            }
            return;
        }

        if (getPath() == null) {
            if (target.getPath() != null) {
                setRealpath(target.getPath());
            } else {
                target.setRealpath(getRealpath());
                target.setPath(getRealpath());
            }
            target.setRoot(getRoot());
            this.target = target;
            target.getLinksIn().add(this);
            setPackage(target.getPackage());
            return;
        }

        delistFromMeta();
        setPackage(target.getPackage());
        setRealpath(target.getPath());
        refreshLocation();

        target.setRoot(getRoot());
    }

    @Override
    public @Nullable String getResolved() {
        String path = getPath();
        String realpath = getRealpath();
        return path != null && realpath != null ? "file:" + Relpath.relpath(PosixPath.dirname(path), realpath) : null;
    }

    @Override
    public void setResolved(@Nullable Object resolved) {
    }

    @Override
    void loadDeps() {
    }

    @Override
    void recalculateOutEdgesOverrides() {
        OverrideSet overrides = getOverrides();
        if (target == null || overrides == null) {
            return;
        }
        boolean hasMatchingRule = false;
        for (OverrideSet rule : overrides.getRuleset().values()) {
            if (target.getEdgesOut().has(rule.getName())) {
                hasMatchingRule = true;
                break;
            }
        }
        if (!hasMatchingRule) {
            return;
        }
        target.updateOverridesEdgeInAdded(overrides);
    }

    @Override
    public CaseInsensitiveMap<Node> getChildren() {
        return new CaseInsensitiveMap<>();
    }

    @Override
    public boolean isLink() {
        return true;
    }
}
