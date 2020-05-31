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
package org.openrewrite.xml.search.maven;

import org.openrewrite.Tree;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlSourceVisitor;
import org.openrewrite.xml.tree.Xml;

public class ReadPom extends XmlSourceVisitor<MavenPom> {
    private final XPathMatcher dependencyMatcher = new XPathMatcher(
            "/project/dependencies/dependency");

    private final XPathMatcher propertyMatcher = new XPathMatcher(
            "/project/properties/*");

    public ReadPom() {
        super("maven.ReadPom");
        setCursoringOn();
    }

    @Override
    public MavenPom defaultTo(Tree t) {
        return new MavenPom();
    }

    @Override
    public MavenPom reduce(MavenPom r1, MavenPom r2) {
        return r1.merge(r2);
    }

    @Override
    public MavenPom visitTag(Xml.Tag tag) {
        if (dependencyMatcher.matches(getCursor())) {
            return new MavenPom().withDependency(new Dependency(
                    tag.getChildValue("groupId"),
                    tag.getChildValue("artifactId"),
                    tag.getChildValue("version"),
                    tag.getChildValue("scope")
            ));
        }

        if (propertyMatcher.matches(getCursor())) {
            return tag.getValue()
                    .map(value -> new MavenPom().withProperty(tag.getName(), value))
                    .orElseGet(MavenPom::new);
        }

        return super.visitTag(tag);
    }
}
