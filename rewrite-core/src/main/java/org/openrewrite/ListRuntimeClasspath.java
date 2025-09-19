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
package org.openrewrite;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.openrewrite.table.ClasspathReport;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

public class ListRuntimeClasspath extends ScanningRecipe<Integer> {
    transient ClasspathReport report = new ClasspathReport(this);

    @Override
    public String getDisplayName() {
        return "List runtime classpath";
    }

    @Override
    public String getDescription() {
        return "A diagnostic utility which emits the runtime classpath to a data table.";
    }

    @Override
    public Integer getInitialValue(ExecutionContext ctx) {
        return 0;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Integer acc) {
        return TreeVisitor.noop();
    }

    @Override
    public Collection<? extends SourceFile> generate(Integer acc, ExecutionContext ctx) {
        try (ScanResult result = new ClassGraph().scan()) {
            ResourceList resources = result.getResourcesWithExtension(".jar");
            Map<String, List<Resource>> classpathEntriesWithJarResources = resources.stream()
                    .collect(groupingBy(it -> it.getClasspathElementURI().toString()));
            for (URI classPathUri : result.getClasspathURIs()) {
                List<Resource> jarResources = classpathEntriesWithJarResources.get(classPathUri.toString());
                if (jarResources == null || jarResources.isEmpty()) {
                    report.insertRow(ctx, new ClasspathReport.Row(classPathUri.toString(), ""));
                } else {
                    for (Resource r : jarResources) {
                        report.insertRow(ctx, new ClasspathReport.Row(classPathUri.toString(), r.getPath()));
                    }
                }
            }
        }
        return emptyList();
    }
}
