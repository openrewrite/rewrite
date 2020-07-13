/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.tree;

import org.openrewrite.Refactor;
import org.openrewrite.xml.ChangeTagContent;
import org.openrewrite.xml.search.FindTag;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Helper to map lists of {@link Xml.Tag} to some model object that holds additional context about the contents of each tag,
 * e.g. resolved dependency version for a Maven dependency tag.
 *
 * @param <M> The model type.
 */
class MemoizedTags<M> {
    private final Xml.Tag root;
    private final String pathToModel;
    private final Function<Xml.Tag, M> buildModel;
    private final Function<M, Xml.Tag> modelToTag;

    private final Xml.Tag parent;

    private List<M> memoized;

    protected MemoizedTags(Xml.Tag root, String pathToModel, Function<Xml.Tag, M> buildModel,
                           Function<M, Xml.Tag> modelToTag) {
        this.root = root;
        this.pathToModel = pathToModel;
        this.buildModel = buildModel;
        this.modelToTag = modelToTag;
        this.parent = new FindTag(pathToModel.substring(0, pathToModel.lastIndexOf('/')))
                .visit(root);
    }

    public List<M> getModels() {
        return memoizeIfNecessary();
    }

    public Optional<Xml.Tag> with(List<M> maybeMutated) {
        if (root == null) {
            throw new IllegalStateException("Expecting parent tag to already exist");
        }

        synchronized (this) {
            if (maybeMutated != getModels()) {
                Map<Xml.Tag, M> modelsByTag = new HashMap<>(getModels().size());
                getModels().forEach(m -> modelsByTag.put(modelToTag.apply(m), m));

                List<Content> content = new ArrayList<>();
                for (M model : maybeMutated) {
                    M memoizedModel = modelsByTag.get(modelToTag.apply(model));
                    if (memoizedModel == null) {
                        content.add(modelToTag.apply(model));
                    } else {
                        content.add(modelToTag.apply(model));
                    }
                }

                memoized = maybeMutated;
                return Optional.of(new Refactor<>(root)
                        .visit(new ChangeTagContent(parent, content))
                        .fix()
                        .getFixed()
                );
            }
        }

        return Optional.empty();
    }

    private List<M> memoizeIfNecessary() {
        if (memoized == null) {
            List<M> memoizable = Optional.ofNullable(root)
                    .map(parent -> new FindTags(pathToModel).visit(root))
                    .map(tags -> tags.stream().map(buildModel).collect(toList()))
                    .orElse(emptyList());

            synchronized (this) {
                if (memoized == null) {
                    memoized = memoizable;
                }
            }
        }

        return memoized;
    }
}
