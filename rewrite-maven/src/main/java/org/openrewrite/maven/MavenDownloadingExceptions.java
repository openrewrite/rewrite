/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import lombok.Getter;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.maven.MavenVisitor.*;

@Getter
public class MavenDownloadingExceptions extends Exception {
    /**
     * Exceptions tied to one or more direct dependencies, parent, or
     * dependency management dependencies.
     */
    private final List<MavenDownloadingException> exceptions = new ArrayList<>();

    public static MavenDownloadingExceptions append(@Nullable MavenDownloadingExceptions current,
                                                    MavenDownloadingException exception) {
        if (current == null) {
            current = new MavenDownloadingExceptions();
        }
        current.addSuppressed(exception);
        current.exceptions.add(exception);
        return current;
    }

    public static MavenDownloadingExceptions append(@Nullable MavenDownloadingExceptions current,
                                                    MavenDownloadingExceptions exceptions) {
        if (current == null) {
            current = new MavenDownloadingExceptions();
        }
        exceptions.getExceptions().forEach(current::addSuppressed);
        current.exceptions.addAll(exceptions.getExceptions());
        return current;
    }

    public Xml.Document warn(Xml.Document document) {
        Map<GroupArtifact, List<MavenDownloadingException>> byGav = new HashMap<>();
        for (MavenDownloadingException exception : exceptions) {
            byGav.computeIfAbsent(new GroupArtifact(exception.getRoot().getGroupId(),
                    exception.getRoot().getArtifactId()), ga -> new ArrayList<>()).add(exception);
        }

        return (Xml.Document) new XmlIsoVisitor<Integer>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, Integer integer) {
                return sourceFile instanceof Xml.Document;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, Integer integer) {
                Xml.Tag t = tag;
                for (GroupArtifact ga : byGav.keySet()) {
                    boolean hasException = (DEPENDENCY_MATCHER.matches(getCursor()) || MANAGED_DEPENDENCY_MATCHER.matches(getCursor()) || PARENT_MATCHER.matches(getCursor())) &&
                                           tag.getChildValue("groupId").map(a -> ga.getGroupId().equals(a)).orElse(false) &&
                                           tag.getChildValue("artifactId").map(a -> ga.getArtifactId().equals(a)).orElse(false);
                    if (hasException) {
                        for (MavenDownloadingException exception : byGav.get(ga)) {
                            t = Markup.warn(t, exception);
                        }
                    }
                }
                return super.visitTag(t, integer);
            }
        }.visitNonNull(document, 0);
    }
}
