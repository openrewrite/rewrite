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
        private final String pkgName;

        public FindPackageVisitor(String pkgName) {
            this.pkgName = pkgName;
        }

        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {
            J j = super.postVisit(tree, executionContext);
            if (j instanceof TypedTree) {
                j = maybeAddMarker((TypedTree) j, executionContext);
            }
            return j;
        }

        private J maybeAddMarker(TypedTree tree, ExecutionContext executionContext) {
            if (getCursor().getParent() != null && getCursor().getParent().getValue() instanceof J.VariableDeclarations.NamedVariable) {
                return tree;
            }
            if (isSearchPackage(tree.getType())) {
                AtomicBoolean markerExists = new AtomicBoolean(false);
                new HasSearchMarkerVisitor(markerExists).visit(tree, executionContext);
                if (!markerExists.get()) {
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

        private static class HasSearchMarkerVisitor extends JavaIsoVisitor<ExecutionContext> {
            private final AtomicBoolean markerExists;

            private HasSearchMarkerVisitor(AtomicBoolean markerExists) {
                this.markerExists = markerExists;
            }

            @Override
            public <M extends Marker> M visitMarker(Marker marker, ExecutionContext executionContext) {
                M m = super.visitMarker(marker, executionContext);
                if (m instanceof SearchResult) {
                    markerExists.set(true);
                }
                return m;
            }
        }
    }
}
