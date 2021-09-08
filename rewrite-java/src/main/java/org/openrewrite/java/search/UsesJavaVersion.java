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
package org.openrewrite.java.search;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;

import java.util.*;

import static org.openrewrite.Tree.randomId;

public class UsesJavaVersion<P> extends JavaIsoVisitor<P> {
    @SuppressWarnings("ConstantConditions")
    private static final Marker JAVA_VERSION_SEARCH_RESULT = new JavaSearchResult(randomId(), null, null);

    int majorVersionMin;
    int majorVersionMax;

    public UsesJavaVersion(int majorVersion) {
        this.majorVersionMin = majorVersion;
        this.majorVersionMax = Integer.MAX_VALUE;
    }

    public UsesJavaVersion(int majorVersionMin, int majorVersionMax) {
        this.majorVersionMin = majorVersionMin;
        this.majorVersionMax = majorVersionMax;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, P p) {
        Optional<JavaVersion> javaVersion = cu.getMarkers().findFirst(JavaVersion.class);
        if (javaVersion.isPresent() && isVersionInRange(javaVersion.get().getMajorVersion())) {
            return cu.withMarkers(cu.getMarkers().addIfAbsent(JAVA_VERSION_SEARCH_RESULT));
        }

        return cu;
    }

    private boolean isVersionInRange(int majorVersion) {
        return majorVersionMin > 0 && majorVersionMin <= majorVersion && majorVersion <= majorVersionMax;
    }
}
