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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class Maven extends Xml.Document {
    @Getter
    private final transient MavenResolutionResult mavenResolutionResult;

    @Getter
    private final transient List<MavenResolutionResult> modules;

    public Maven(Document document) {
        super(
                document.getId(),
                document.getSourcePath(),
                document.getPrefix(),
                document.getMarkers(),
                document.getProlog(),
                document.getRoot(),
                document.getEof()
        );

        mavenResolutionResult = document.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
        assert mavenResolutionResult != null;

        modules = document.getMarkers().findFirst(Modules.class)
                .map(Modules::getModules)
                .orElse(emptyList());
    }

    @JsonCreator
    public Maven(UUID id, Path sourcePath, String prefix, Markers markers, Prolog prolog, Tag root, String eof) {
        super(id, sourcePath, prefix, markers, prolog, root, eof);
        mavenResolutionResult = markers.findFirst(MavenResolutionResult.class).orElse(null);
        assert mavenResolutionResult != null;

        modules = markers.findFirst(Modules.class)
                .map(Modules::getModules)
                .orElse(emptyList());
    }

    public Maven withMavenResolutionResult(MavenResolutionResult model) {
        return withMarkers(getMarkers().withMarkers(ListUtils.map(getMarkers().getMarkers(), m ->
                m instanceof MavenResolutionResult ? model : m)));
    }

    public Maven withModules(List<MavenResolutionResult> modules) {
        return withMarkers(getMarkers().withMarkers(ListUtils.map(getMarkers().getMarkers(), m ->
                m instanceof Modules ? ((Modules) m).withModules(modules) : m)));
    }

    public static List<Path> getMavenPoms(Path projectDir, ExecutionContext ctx) {
        return getSources(projectDir, ctx, "pom.xml").stream()
                .filter(p -> p.getFileName().toString().equals("pom.xml") &&
                        !p.toString().contains("/src/"))
                .collect(Collectors.toList());
    }

    public List<Path> getJavaSources(Path projectDir, ExecutionContext ctx) {
        if (!"war".equals(mavenResolutionResult.getPom().getPackaging()) &&
                !"jar".equals(mavenResolutionResult.getPom().getPackaging()) &&
                !"bundle".equals(mavenResolutionResult.getPom().getPackaging())) {
            return emptyList();
        }
        return getSources(projectDir.resolve(getSourcePath()).getParent().resolve(Paths.get("src", "main", "java")),
                ctx, ".java");
    }

    public List<Path> getTestJavaSources(Path projectDir, ExecutionContext ctx) {
        if (!"jar".equals(mavenResolutionResult.getPom().getPackaging()) && !"bundle".equals(mavenResolutionResult.getPom().getPackaging())) {
            return emptyList();
        }
        return getSources(projectDir.resolve(getSourcePath()).getParent().resolve(Paths.get("src", "test", "java")),
                ctx, ".java");
    }

    public List<Path> getResources(Path projectDir, ExecutionContext ctx) {
        if (!"jar".equals(mavenResolutionResult.getPom().getPackaging()) && !"bundle".equals(mavenResolutionResult.getPom().getPackaging())) {
            return emptyList();
        }
        return getSources(projectDir.resolve(getSourcePath()).getParent().resolve(Paths.get("src", "main", "resources")),
                ctx, ".properties", ".xml", ".yml", ".yaml");
    }

    public List<Path> getTestResources(Path projectDir, ExecutionContext ctx) {
        if (!"jar".equals(mavenResolutionResult.getPom().getPackaging()) && !"bundle".equals(mavenResolutionResult.getPom().getPackaging())) {
            return emptyList();
        }
        return getSources(projectDir.resolve(getSourcePath()).getParent().resolve(Paths.get("src", "test", "resources")),
                ctx, ".properties", ".xml", ".yml", ".yaml");
    }

    private static List<Path> getSources(Path srcDir, ExecutionContext ctx, String... fileTypes) {
        if (!srcDir.toFile().exists()) {
            return emptyList();
        }

        BiPredicate<Path, java.nio.file.attribute.BasicFileAttributes> predicate = (p, bfa) ->
                bfa.isRegularFile() && Arrays.stream(fileTypes).anyMatch(type -> p.getFileName().toString().endsWith(type));
        try {
            return Files.find(srcDir, 999, predicate).collect(Collectors.toList());
        } catch (IOException e) {
            ctx.getOnError().accept(e);
            return emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        if (v instanceof MavenVisitor && p instanceof ExecutionContext) {
            return (R) ((MavenVisitor) v).visitMaven(this, (ExecutionContext) p);
        } else if (v instanceof XmlVisitor) {
            return super.accept(v, p);
        }
        return v.defaultValue(this, p);
    }

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof XmlVisitor;
    }

    @Override
    public Maven withRoot(Tag root) {
        Document m = super.withRoot(root);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m);
    }

    @Override
    public Maven withMarkers(Markers markers) {
        Document m = super.withMarkers(markers);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m);
    }

    @Override
    public Maven withPrefix(String prefix) {
        Document m = super.withPrefix(prefix);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m);
    }

    @Override
    public Maven withProlog(Prolog prolog) {
        Document m = super.withProlog(prolog);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m);
    }

    public Maven withMavenResolutionResult(ResolvedPom model) {
        MavenResolutionResult withMavenResolutionResult = mavenResolutionResult.withPom(model);
        return withMarkers(getMarkers().computeByType(withMavenResolutionResult, (old, n) -> n));
    }
}
