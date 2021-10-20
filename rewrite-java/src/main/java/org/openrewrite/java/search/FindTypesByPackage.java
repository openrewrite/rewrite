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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindTypesByPackage extends Recipe {
    @Option(displayName = "Package name",
            description = "The package name to search for.",
            example = "com.yourorg")
    String packageName;

    @Override
    public String getDisplayName() {
        return "Find Types by package";
    }

    @Override
    public String getDescription() {
        return "A recipe for finding Types by package name.";
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(packageName + ".*");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindPackageVisitor(packageName);
    }

    private static class FindPackageVisitor extends JavaVisitor<ExecutionContext> {
        private static final HasSearchMarkerVisitor HAS_SEARCH_MARKER_VISITOR = new HasSearchMarkerVisitor();
        private final String pkgName;

        public FindPackageVisitor(String pkgName) {
            this.pkgName = pkgName;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
            J j = super.postVisit(tree, executionContext);
            if (j instanceof TypedTree) {
                j = maybeAddMarker((TypedTree) j);
            }
            return j;
        }

        private J maybeAddMarker(TypedTree tree) {
            if (isSearchPackage(tree.getType())) {
                AtomicBoolean b = new AtomicBoolean(false);
                HAS_SEARCH_MARKER_VISITOR.visit(tree, b);
                if (!b.get()) {
                    tree = tree.withMarkers(tree.getMarkers().searchResult());
                }
            }
            return tree;
        }

        private boolean isSearchPackage(@Nullable JavaType type) {
            if (type != null) {
                JavaType.FullyQualified fq;
                if (type instanceof JavaType.Method) {
                    fq = TypeUtils.asFullyQualified(((JavaType.Method) type).getDeclaringType());
                } else {
                    fq = TypeUtils.asFullyQualified(type);
                }
                return fq != null && fq.getPackageName().startsWith(pkgName);
            }
            return false;
        }

        private static class HasSearchMarkerVisitor extends JavaIsoVisitor<AtomicBoolean> {
            @Override
            public <M extends Marker> M visitMarker(Marker marker, AtomicBoolean atomicBoolean) {
                M m = super.visitMarker(marker, atomicBoolean);
                if (m instanceof SearchResult) {
                    atomicBoolean.set(true);
                }
                return m;
            }
        }
    }
}
