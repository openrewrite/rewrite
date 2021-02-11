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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class Maven extends Xml.Document {
    private final transient Pom model;
    private final transient Collection<Pom> modules;
    private final transient MavenSettings settings;

    public Maven(Xml.Document document, @Nullable MavenSettings settings) {
        super(
                document.getId(),
                document.getSourcePath(),
                document.getPrefix(),
                document.getMarkers(),
                document.getProlog(),
                document.getRoot(),
                document.getEof()
        );

        this.settings = settings;

        model = document.getMarkers().findFirst(Pom.class).orElse(null);
        assert model != null;

        modules = document.getMarkers().findFirst(Modules.class)
                .map(Modules::getModules)
                .orElse(emptyList());
    }

    public static List<Path> getMavenPoms(Path projectDir, ExecutionContext ctx) {
        return getSources(projectDir, ctx, "pom.xml");
    }

    public List<Path> getJavaSources(ExecutionContext ctx) {
        if (!"jar".equals(model.getPackaging())) {
            return emptyList();
        }
        return getSources(getSourcePath().getParent().resolve(Paths.get("src", "main", "java")),
                ctx, ".java");
    }

    public List<Path> getTestJavaSources(ExecutionContext ctx) {
        if (!"jar".equals(model.getPackaging())) {
            return emptyList();
        }
        return getSources(getSourcePath().getParent().resolve(Paths.get("src", "test", "java")),
                ctx, ".java");
    }

    public List<Path> getResources(ExecutionContext ctx) {
        if (!"jar".equals(model.getPackaging())) {
            return emptyList();
        }
        return getSources(getSourcePath().getParent().resolve(Paths.get("src", "main", "resources")),
                ctx, ".properties", ".xml", ".yml", ".yaml");
    }

    public List<Path> getTestResources(ExecutionContext ctx) {
        if (!"jar".equals(model.getPackaging())) {
            return emptyList();
        }
        return getSources(getSourcePath().getParent().resolve(Paths.get("src", "test", "resources")),
                ctx, ".properties", ".xml", ".yml", ".yaml");
    }

    private static List<Path> getSources(Path srcDir, ExecutionContext ctx, String... fileTypes) {
        BiPredicate<Path, java.nio.file.attribute.BasicFileAttributes> predicate = (p, bfa) ->
                bfa.isRegularFile() && Arrays.stream(fileTypes).anyMatch(type -> p.getFileName().toString().endsWith(type));
        try {
            return Files.find(srcDir, 999, predicate).collect(Collectors.toList());
        } catch (IOException e) {
            ctx.getOnError().accept(e);
            return emptyList();
        }
    }

    public Pom getModel() {
        return model;
    }

    public Collection<Pom> getModules() {
        return modules;
    }

    public MavenSettings getSettings() {
        return settings;
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
    public Maven withRoot(Tag root) {
        Document m = super.withRoot(root);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, settings);
    }

    @Override
    public Maven withMarkers(Markers markers) {
        Document m = super.withMarkers(markers);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, settings);
    }

    @Override
    public Maven withPrefix(String prefix) {
        Document m = super.withPrefix(prefix);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, settings);
    }

    @Override
    public Maven withProlog(Prolog prolog) {
        Document m = super.withProlog(prolog);
        if (m instanceof Maven) {
            return (Maven) m;
        }
        return new Maven(m, settings);
    }

    public Maven withModel(Pom model) {
        return withMarkers(getMarkers().compute(model, (old, n) -> n));
    }
}
