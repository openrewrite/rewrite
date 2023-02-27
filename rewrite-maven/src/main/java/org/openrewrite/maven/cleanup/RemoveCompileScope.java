/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;

public class RemoveCompileScope extends Recipe {

    @Override
    public String getDisplayName() {
        return "Removes the compile scope";
    }

    @Override
    public String getDescription() {
        return "Removes the compile scope for all the dependencies as it is the default scope,and it is redundant.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        Map<String,String> seen = new HashMap<>();

        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {

                if (isManagedDependencyTag()){
                    String groupId = getGroupId(tag);
                    String artifactId = getArtifactId(tag);
                    String scope = getScope(tag);

                    if (groupId != null && artifactId!= null && scope!= null){
                        seen.put(groupId + artifactId, scope);
                    }
                }
                else if (isDependencyTag()) {
                    String groupId = getGroupId(tag);
                    String artifactId = getArtifactId(tag);
                    String scope = getScope(tag);
                    String seenScope = seen.get(groupId + artifactId);

                    if ("compile".equalsIgnoreCase(seenScope) || seenScope == null && "compile".equals(scope) ){
                        doAfterVisit(new RemoveContentVisitor<>(tag.getChild("scope").get(), false));
                    }

                }

                return super.visitTag(tag, ctx);
            }
        };
    }

    @Nullable
    private static String getScope(Xml.Tag tag) {
        return tag.getChild("scope").flatMap(Xml.Tag::getValue).orElse(null);
    }

    @Nullable
    private static String getArtifactId(Xml.Tag tag) {
        return tag.getChild("artifactId").flatMap(Xml.Tag::getValue).orElse(null);
    }

    @Nullable
    private static String getGroupId(Xml.Tag tag) {
        return tag.getChild("groupId").flatMap(Xml.Tag::getValue).orElse(null);
    }
}
