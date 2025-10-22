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
package org.openrewrite.gradle.internal;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.DependencyNotation;

import java.util.*;

public class InsertDependencyComparator implements Comparator<Statement> {
    private final Map<Statement, Float> positions = new LinkedHashMap<>();

    @Getter
    @Nullable
    private Statement afterDependency;

    @Getter
    @Nullable
    private Statement beforeDependency;

    public InsertDependencyComparator(List<Statement> statements, J.MethodInvocation dependencyToAdd) {
        for (int i = 0, len = statements.size(); i < len; i++) {
            positions.put(statements.get(i), (float) i);
        }

        // Make a copy of statements with only dependencies
        List<Statement> dependencies = new ArrayList<>(statements.size() + 1);
        for (Statement s : statements) {
            if (s instanceof J.MethodInvocation || (s instanceof J.Return && ((J.Return) s).getExpression() instanceof J.MethodInvocation)) {
                dependencies.add(s);
            }
        }

        if (dependencies.isEmpty()) {
            positions.put(dependencyToAdd, statements.size() + 0.5f);
            return;
        }

        // Fill `afterDependency` and `beforeDependency`
        dependencies.add(dependencyToAdd);
        dependencies.sort(dependenciesComparator);
        for (int i = 0, len = dependencies.size(); i < len; i++) {
            if (dependencyToAdd == dependencies.get(i)) {
                if (i > 0) {
                    afterDependency = dependencies.get(i - 1);
                }
                if (i + 1 < dependencies.size()) {
                    beforeDependency = dependencies.get(i + 1);
                }
                break;
            }
        }

        // Put `dependencyToAdd` at the proper place in the positions map
        boolean isFirst = afterDependency == null;
        for (float f = isFirst ? 0 : positions.get(afterDependency); f < statements.size(); f++) {
            Statement s = statements.get((int) f);
            if (!(s instanceof J.MethodInvocation || (s instanceof J.Return && ((J.Return) s).getExpression() instanceof J.MethodInvocation))) {
                continue;
            }
            positions.put(dependencyToAdd, positions.get(s) + (isFirst ? -0.5f : 0.5f));
            break;
        }
    }

    @Override
    public int compare(Statement o1, Statement o2) {
        return positions.get(o1).compareTo(positions.get(o2));
    }

    private static final Comparator<Statement> dependenciesComparator = (s1, s2) -> {
        J.MethodInvocation d1;
        if (s1 instanceof J.Return) {
            d1 = (J.MethodInvocation) ((J.Return) s1).getExpression();
        } else {
            d1 = (J.MethodInvocation) s1;
        }

        J.MethodInvocation d2;
        if (s2 instanceof J.Return) {
            d2 = (J.MethodInvocation) ((J.Return) s2).getExpression();
        } else {
            d2 = (J.MethodInvocation) s2;
        }

        assert d1 != null && d2 != null;
        String configuration1 = d1.getSimpleName();
        String configuration2 = d2.getSimpleName();
        if (!configuration1.equals(configuration2)) {
            return configuration1.compareTo(configuration2);
        }

        String groupId1 = getEntry("group", d1).orElse("");
        String groupId2 = getEntry("group", d2).orElse("");
        if (!groupId1.equals(groupId2)) {
            return comparePartByPart(groupId1, groupId2);
        }

        String artifactId1 = getEntry("name", d1).orElse("");
        String artifactId2 = getEntry("name", d2).orElse("");
        if (!artifactId1.equals(artifactId2)) {
            return comparePartByPart(artifactId1, artifactId2);
        }

        String classifier1 = getEntry("classifier", d1).orElse(null);
        String classifier2 = getEntry("classifier", d2).orElse(null);
        if (classifier1 == null && classifier2 != null) {
            return -1;
        } else if (classifier1 != null) {
            if (classifier2 == null) {
                return 1;
            }
            if (!classifier1.equals(classifier2)) {
                return classifier1.compareTo(classifier2);
            }
        }

        // in every case imagined so far, group and artifact comparison are enough,
        // so this is just for completeness
        return getEntry("version", d1).orElse("")
                .compareTo(getEntry("version", d2).orElse(""));
    };

    private static Optional<String> getEntry(String entry, J.MethodInvocation invocation) {
        if (invocation.getArguments().get(0) instanceof J.Literal) {
            Object value = ((J.Literal) invocation.getArguments().get(0)).getValue();
            if(value == null) {
                return Optional.empty();
            }
            Dependency dependency = DependencyNotation.parse((String) value);
            if(dependency == null) {
                return Optional.empty();
            }
            switch (entry) {
                case "group":
                    return Optional.ofNullable(dependency.getGroupId());
                case "name":
                    //noinspection OptionalOfNullableMisuse
                    return Optional.ofNullable(dependency.getArtifactId());
                case "version":
                    return Optional.ofNullable(dependency.getVersion());
                case "classifier":
                    return Optional.ofNullable(dependency.getClassifier());
            }
        } else if (invocation.getArguments().get(0) instanceof G.MapEntry) {
            for (Expression e : invocation.getArguments()) {
                if (!(e instanceof G.MapEntry)) {
                    continue;
                }

                G.MapEntry mapEntry = (G.MapEntry) e;
                if (!(mapEntry.getKey() instanceof J.Literal && mapEntry.getValue() instanceof J.Literal)) {
                    continue;
                }

                if (entry.equals(((J.Literal) mapEntry.getKey()).getValue())) {
                    return Optional.ofNullable((String) ((J.Literal) mapEntry.getValue()).getValue());
                }
            }
        }

        return Optional.empty();
    }

    private static int comparePartByPart(String d1, String d2) {
        String[] d1Parts = d1.split("[.-]");
        String[] d2Parts = d2.split("[.-]");

        for (int i = 0; i < Math.min(d1Parts.length, d2Parts.length); i++) {
            if (!d1Parts[i].equals(d2Parts[i])) {
                return d1Parts[i].compareTo(d2Parts[i]);
            }
        }

        return d1Parts.length - d2Parts.length;
    }
}
