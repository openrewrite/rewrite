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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeParentVersion extends Recipe {

    private static final XPathMatcher PARENT_VERSION_MATCHER = new XPathMatcher("/project/parent/version");

    private final String groupId;
    private final String artifactId;
    private final String newVersion;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeParentVersionVisitor();
    }

    private class ChangeParentVersionVisitor extends MavenVisitor<ExecutionContext> {

        private ChangeParentVersionVisitor() {
            setCursoringOn();
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (PARENT_VERSION_MATCHER.matches(getCursor())) {
                Xml.Tag parent = getCursor().getParentOrThrow().getValue();
                if (groupId.equals(parent.getChildValue("groupId").orElse(null)) &&
                        artifactId.equals(parent.getChildValue("artifactId").orElse(null)) &&
                        !newVersion.equals(tag.getValue().orElse(null))) {
                    doAfterVisit(new ChangeTagValueVisitor<>(tag, newVersion));
                }
            }
            return super.visitTag(tag, ctx);
        }
    }
}
