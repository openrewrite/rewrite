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
package org.openrewrite.java.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Incubating(since = "7.0.0")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class JavaProvenance implements Marker {
    UUID id;
    String projectName;
    String sourceSetName;
    BuildTool buildTool;
    JavaVersion javaVersion;
    Set<JavaType.FullyQualified> classpath;

    @Nullable
    Publication publication;

    public static JavaProvenance build(
            @Nullable String projectName,
            String sourceSetName,
            BuildTool buildTool,
            JavaVersion javaVersion,
            Iterable<Path> classpath,
            Publication publication) {

        Set<JavaType.FullyQualified> fqns = new HashSet<>();
        if (classpath.iterator().hasNext()) {
            for (ClassInfo aClass : new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .scan()
                    .getAllClasses()) {
                fqns.add(JavaType.Class.build(aClass.getName()));
            }
        }

        return new JavaProvenance(
                UUID.randomUUID(),
                projectName,
                sourceSetName,
                buildTool,
                javaVersion,
                fqns,
                publication);
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class BuildTool {
        Type type;
        String version;

        public enum Type {
            Gradle,
            Maven
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Publication {
        String groupId;
        String artifactId;
        String version;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class JavaVersion {
        String createdBy;
        String vmVendor;
        String sourceCompatibility;
        String targetCompatibility;
    }
}
