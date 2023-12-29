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
package org.openrewrite.maven;

import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

/**
 * Helps to insert a new POM element in the most idiomatic
 * place possible while preserving the existing order of the already
 * present elements.
 * <p>
 * So we'll prefer to insert a dependencies block after
 * GAV coordinates, SCM, properties, but before plugins.
 * "After" ordering preference takes priority over "before".
 */
public class MavenTagInsertionComparator implements Comparator<Content> {
    static final List<String> canonicalOrdering = Arrays.asList(
            "modelVersion",
            "parent",
            "groupId",
            "artifactId",
            "version",
            "relativePath",
            "packaging",
            "name",
            "description",
            "url",
            "inceptionYear",
            "organization",
            "licenses",
            "developers",
            "contributors",
            "mailingLists",
            "prerequisites",
            "modules",
            "scm",
            "issueManagement",
            "ciManagement",
            "distributionManagement",
            "properties",
            "dependencyManagement",
            "dependencies",
            "repositories",
            "pluginRepositories",
            "build",
            "configuration",
            "reports",
            "reporting",
            "profiles"
    );

    private final Map<Content, Integer> existingIndices = new IdentityHashMap<>();

    public MavenTagInsertionComparator(List<? extends Content> existingTags) {
        for (int i = 0; i < existingTags.size(); i++) {
            existingIndices.put(existingTags.get(i), i);
        }
    }

    @Override
    public int compare(Content c1,Content c2) {
        if (!(c1 instanceof Xml.Tag) || !(c2 instanceof Xml.Tag)) {
            return 1;
        }

        Xml.Tag t1 = (Xml.Tag) c1;
        Xml.Tag t2 = (Xml.Tag) c2;
        int i1 = existingIndices.getOrDefault(t1, -1);
        int i2 = existingIndices.getOrDefault(t2, -1);

        if (i1 == -1) {
            if (i2 == -1) {
                return canonicalOrdering.indexOf(t1.getName()) -
                        canonicalOrdering.indexOf(t2.getName());
            } else {
                // trying to place a new t1
                for (int i = 0; i < canonicalOrdering.indexOf(t2.getName()); i++) {
                    if (canonicalOrdering.get(i).equals(t1.getName())) {
                        return -1;
                    }
                }
                return 1;
            }
        } else {
            if (i2 == -1) {
                // trying to place a new t2
                for (int i = 0; i < canonicalOrdering.indexOf(t1.getName()); i++) {
                    if (canonicalOrdering.get(i).equals(t2.getName())) {
                        return 1;
                    }
                }
                return -1;
            } else {
                return i1 - i2;
            }
        }
    }
}
