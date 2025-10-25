/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class ManagedToRuntimeDependencies extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert managed dependencies to runtime dependencies";
    }

    @Override
    public String getDescription() {
        return "This recipe processes Maven POMs, converting all `<dependencyManagement>` entries into runtime scoped `<dependencies>` entries. " +
               "Import scoped BOMs (like jackson-bom) are left unmodified in `<dependencyManagement>`. " +
               "Some style guidelines prefer that `<dependencyManagement>` be used only for BOMs. " +
               "This maintain that style while avoiding introducing new symbols onto the compile classpath unintentionally.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                List<Xml.Tag> managedDepsToConvert = new ArrayList<>();
                Xml.Document doc = (Xml.Document) new MavenIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        if (isManagedDependencyTag()) {
                            String scope = tag.getChildValue("scope").orElse(null);
                            if (!"import".equals(scope)) {
                                managedDepsToConvert.add(tag);
                                //noinspection DataFlowIssue
                                return null;
                            }
                        }
                        return super.visitTag(tag, ctx);
                    }
                }.visitNonNull(document, ctx);

                if (managedDepsToConvert.isEmpty()) {
                    return doc;
                }

                doAfterVisit(new MavenIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        if ("project".equals(tag.getName())) {
                            Xml.Tag dependencies = tag.getChild("dependencies").orElse(null);

                            if (dependencies == null) {
                                Xml.Tag dependencyManagement = tag.getChild("dependencyManagement").orElse(null);
                                String indentToUse = dependencyManagement != null ?
                                    dependencyManagement.getPrefix() :
                                    "\n    ";

                                Xml.Tag newDependencies = Xml.Tag.build("<dependencies/>")
                                    .withPrefix(indentToUse);

                                doAfterVisit(new AddToTagVisitor<>(tag, newDependencies,
                                        new MavenTagInsertionComparator(tag.getContent())));

                                // Add each converted dependency to the new dependencies section
                                for (Xml.Tag depToConvert : managedDepsToConvert) {
                                    Xml.Tag convertedDep = createRuntimeDependency(depToConvert);
                                    doAfterVisit(new AddToTagVisitor<>(newDependencies, convertedDep));
                                }
                            } else {
                                // Add to existing dependencies section
                                for (Xml.Tag depToConvert : managedDepsToConvert) {
                                    Xml.Tag convertedDep = createRuntimeDependency(depToConvert);
                                    doAfterVisit(new AddToTagVisitor<>(dependencies, convertedDep));
                                }
                            }
                        }
                        return super.visitTag(tag, ctx);
                    }
                });

                maybeUpdateModel();
                return doc;
            }

            private Xml.Tag createRuntimeDependency(Xml.Tag managedDep) {
                String currentScope = managedDep.getChildValue("scope").orElse(null);
                String targetScope = mapScope(currentScope);

                if (currentScope == null) {
                    // No scope present, add the target scope
                    Xml.Tag scopeTag = Xml.Tag.build("<scope>" + targetScope + "</scope>")
                        .withPrefix("\n        ");
                    return managedDep.withContent(addScopeToContent(managedDep.getContent(), scopeTag));
                } else if (!currentScope.equals(targetScope)) {
                    // Scope present but needs to be changed
                    return managedDep.withContent(replaceScopeInContent(managedDep.getContent(), targetScope));
                } else {
                    // Scope already correct
                    return managedDep;
                }
            }

            private String mapScope(@Nullable String scope) {
                if (scope == null || "compile".equals(scope) || "runtime".equals(scope)) {
                    return "runtime";
                } else if ("test".equals(scope)) {
                    return "test";
                } else if ("provided".equals(scope)) {
                    return "provided";
                } else {
                    // All other scopes remain unchanged
                    return scope;
                }
            }

            private List<Content> addScopeToContent(@Nullable List<? extends Content> content, Xml.Tag scopeTag) {
                if (content == null) {
                   return emptyList();
                }
                boolean scopeAdded = false;

                // Maven's standard element order within a dependency:
                // groupId, artifactId, version, type, classifier, optional, scope, exclusions
                String[] precedingElements = {"optional", "classifier", "type", "version", "artifactId", "groupId"};
                List<Content> newContent = new ArrayList<>(content);

                // Find the right position to insert scope based on standard Maven ordering
                for (String element : precedingElements) {
                    for (int i = 0; i < newContent.size(); i++) {
                        Content item = newContent.get(i);
                        if (item instanceof Xml.Tag && element.equals(((Xml.Tag) item).getName())) {
                            // Find the next XML tag after this element
                            int insertPos = i + 1;
                            while (insertPos < newContent.size() && !(newContent.get(insertPos) instanceof Xml.Tag)) {
                                insertPos++;
                            }
                            newContent.add(insertPos, scopeTag);
                            scopeAdded = true;
                            break;
                        }
                    }
                    if (scopeAdded) {
                        break;
                    }
                }

                if (!scopeAdded) {
                    // If no preceding elements found, add at end (before exclusions if present)
                    for (int i = 0; i < newContent.size(); i++) {
                        Content item = newContent.get(i);
                        if (item instanceof Xml.Tag && "exclusions".equals(((Xml.Tag) item).getName())) {
                            newContent.add(i, scopeTag);
                            scopeAdded = true;
                            break;
                        }
                    }
                    if (!scopeAdded) {
                        newContent.add(scopeTag);
                    }
                }

                return newContent;
            }

            private List<Content> replaceScopeInContent(@Nullable List<? extends Content> content, String targetScope) {
                if (content == null) {
                    return emptyList();
                }
                List<Content> newContent = new ArrayList<>();
                for (Content item : content) {
                    if (item instanceof Xml.Tag && "scope".equals(((Xml.Tag) item).getName())) {
                        Xml.Tag scopeTag = (Xml.Tag) item;
                        newContent.add(scopeTag.withValue(targetScope));
                    } else {
                        newContent.add(item);
                    }
                }
                return newContent;
            }
        };
    }
}
