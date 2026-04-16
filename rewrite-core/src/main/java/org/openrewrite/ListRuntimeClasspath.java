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

import lombok.Getter;
import org.openrewrite.table.ClasspathReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.util.Collections.emptyList;

public class ListRuntimeClasspath extends ScanningRecipe<Integer> {
    transient ClasspathReport report = new ClasspathReport(this);

    @Getter
    final String displayName = "List runtime classpath";

    @Getter
    final String description = "A diagnostic utility which emits the runtime classpath to a data table.";

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
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            for (String entry : classpath.split(System.getProperty("path.separator"))) {
                Path path = Paths.get(entry);
                if (Files.isRegularFile(path) && entry.endsWith(".jar")) {
                    try (JarFile jarFile = new JarFile(path.toFile())) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry jarEntry = entries.nextElement();
                            if (!jarEntry.isDirectory() && jarEntry.getName().endsWith(".jar")) {
                                report.insertRow(ctx, new ClasspathReport.Row(
                                        path.toUri().toString(), jarEntry.getName()));
                            }
                        }
                    } catch (IOException ignored) {
                        report.insertRow(ctx, new ClasspathReport.Row(path.toUri().toString(), ""));
                    }
                } else {
                    report.insertRow(ctx, new ClasspathReport.Row(path.toUri().toString(), ""));
                }
            }
        }
        return emptyList();
    }
}
