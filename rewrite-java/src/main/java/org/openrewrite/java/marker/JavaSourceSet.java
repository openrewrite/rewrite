/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.marker;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.internal.ClassgraphTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@With
public class JavaSourceSet implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    String name;
    List<JavaType.FullyQualified> classpath;

    public static JavaSourceSet build(String sourceSetName, Iterable<Path> classpath,
                                      JavaTypeCache typeCache, ExecutionContext ctx) {

        Map<String, JavaType.FullyQualified> jvmClasses = jvmClasses(typeCache, ctx);
        List<JavaType.FullyQualified> fqns = new ArrayList<>(jvmClasses.values());

        ClassgraphTypeMapping typeMapping = new ClassgraphTypeMapping(typeCache, jvmClasses);

        if (classpath.iterator().hasNext()) {

            try (ScanResult scanResult = new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableAnnotationInfo()
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .ignoreClassVisibility()
                    .ignoreFieldVisibility()
                    .ignoreMethodVisibility()
                    .scan()) {

                for (ClassInfo classInfo : scanResult.getAllClasses()) {
                    try {
                        fqns.add(typeMapping.type(classInfo));
                    } catch (Exception e) {
                        ctx.getOnError().accept(e);
                    }
                }
            }
        }

        return new JavaSourceSet(randomId(), sourceSetName, fqns);
    }

    private static Map<String, JavaType.FullyQualified> jvmClasses(JavaTypeCache javaTypeCache, ExecutionContext ctx) {
        boolean java8 = System.getProperty("java.version").startsWith("1.8");

        ClassInfoList classInfos;
        try (ScanResult scanResult = new ClassGraph()
                .enableMemoryMapping()
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableMethodInfo()
                .enableFieldInfo()
                .enableSystemJarsAndModules()
                .acceptPackages("java")
                .ignoreClassVisibility()
                .ignoreFieldVisibility()
                .ignoreMethodVisibility()
                .scan()) {
            classInfos = scanResult.getAllClasses();
            ClassgraphTypeMapping builder = new ClassgraphTypeMapping(javaTypeCache, emptyMap());
            Map<String, JavaType.FullyQualified> fqns = new HashMap<>(classInfos.size());
            for (ClassInfo classInfo : classInfos) {
                try {
                    fqns.put(classInfo.getName(), builder.type(classInfo));
                } catch (Exception e) {
                    ctx.getOnError().accept(e);
                }
            }
            return fqns;
        }
    }
}
