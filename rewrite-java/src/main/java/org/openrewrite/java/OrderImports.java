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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.internal.FormatFirstClassPrefix;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * This recipe will group and order the imports for a compilation unit using the rules defined by an {@link ImportLayoutStyle}.
 * If a style has not been defined, this recipe will use the default import layout style that is modeled after
 * IntelliJ's default import settings.
 * <p>
 * The @{link {@link OrderImports#removeUnused}} flag (which is defaulted to true) can be used to also remove any
 * imports that are not referenced within the compilation unit.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class OrderImports extends Recipe {

    @Option(displayName = "Remove unused",
            description = "Remove unnecessary imports.",
            required = false)
    @Nullable
    Boolean removeUnused;

    @Option(displayName = "Style YAML",
            description = "An OpenRewrite [style](https://docs.openrewrite.org/concepts-and-explanations/styles) formatted in YAML.",
            //language=yaml
            example = "type: specs.openrewrite.org/v1beta/style\n" +
                    "name: com.yourorg.CustomImportLayout\n" +
                    "styleConfigs:\n" +
                    "  - org.openrewrite.java.style.ImportLayoutStyle:\n" +
                    "      classCountToUseStarImport: 999\n" +
                    "      nameCountToUseStarImport: 999\n" +
                    "      layout:\n" +
                    "        - 'import java.*'\n" +
                    "        - 'import javax.*'\n" +
                    "        - '<blank line>'\n" +
                    "        - 'import all other imports'\n" +
                    "        - '<blank line>'\n" +
                    "        - 'import static all other imports'\n" +
                    "      packagesToFold:\n" +
                    "        - 'import java.awt.*'\n" +
                    "        - 'import static org.junit.jupiter.api.Assertions.*",
            required = false)
    @Nullable
    String style;

    String displayName = "Order imports";

    String description = "Groups and orders import statements. If a [style has been defined](https://docs.openrewrite.org/concepts-and-explanations/styles), this recipe will order the imports " +
                "according to that style. If no style is detected, this recipe will default to ordering imports in " +
                "the same way that IntelliJ IDEA does.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<NamedStyles> namedStyles = styleFromYaml(style);
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                Optional<JavaSourceSet> sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class);
                List<JavaType.FullyQualified> classpath = emptyList();
                if (sourceSet.isPresent()) {
                    classpath = sourceSet.get().getClasspath();
                }

                ImportLayoutStyle importLayoutStyle = importLayoutStyle(cu, namedStyles);
                List<JRightPadded<J.Import>> orderedImports = importLayoutStyle.orderImports(cu.getPadding().getImports(), classpath);

                boolean changed = false;
                if (orderedImports.size() != cu.getImports().size()) {
                    cu = cu.getPadding().withImports(orderedImports);
                    changed = true;
                } else {
                    for (int i = 0; i < orderedImports.size(); i++) {
                        if (orderedImports.get(i) != cu.getPadding().getImports().get(i)) {
                            cu = cu.getPadding().withImports(orderedImports);
                            changed = true;
                            break;
                        }
                    }
                }

                if (Boolean.TRUE.equals(removeUnused)) {
                    doAfterVisit(new RemoveUnusedImports().getVisitor());
                } else if (changed) {
                    doAfterVisit(new FormatFirstClassPrefix<>());
                }

                return withStyles(cu, namedStyles);
            }
        };
    }

    private List<NamedStyles> styleFromYaml(@Nullable String style) {
        if (style == null) {
            return emptyList();
        }
        return new ArrayList<>(new YamlResourceLoader(new ByteArrayInputStream(style.getBytes()),
                URI.create("OrderImports$style"),
                new Properties()).listStyles());
    }

    private ImportLayoutStyle importLayoutStyle(SourceFile cu, List<NamedStyles> parsedStyles) {
        if (parsedStyles.isEmpty()) {
            return Optional.ofNullable(Style.from(ImportLayoutStyle.class, cu))
                    .orElse(IntelliJ.importLayout());
        }
        return requireNonNull(NamedStyles.merge(ImportLayoutStyle.class, parsedStyles));
    }

    private <T extends SourceFile> T withStyles(T cu, List<NamedStyles> parsedStyles) {
        if (parsedStyles.isEmpty()) {
            return cu;
        }
        List<NamedStyles> existingStyles = cu.getMarkers().findAll(NamedStyles.class);
        // Check if all parsed styles already exist (ignoring id)
        boolean allPresent = parsedStyles.stream()
                .allMatch(ns -> existingStyles.stream().anyMatch(es -> namedStylesEqual(ns, es)));
        if (allPresent) {
            return cu;
        }
        // New styles must appear last to take precedence
        List<Marker> markers = cu.getMarkers().getMarkers();
        for (NamedStyles namedStyle : parsedStyles) {
            if (existingStyles.stream().noneMatch(es -> namedStylesEqual(namedStyle, es))) {
                markers = ListUtils.concat(markers, namedStyle);
            }
        }
        return cu.withMarkers(Markers.build(markers));
    }

    private boolean namedStylesEqual(NamedStyles a, NamedStyles b) {
        return a.getName().equals(b.getName());
    }
}
