/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.csharp.marker.MSBuildProject;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A {@code <PackageReference>} declaration in an MSBuild project file, joined with its
 * candidate versions from the {@link MSBuildProject} marker. Analog of
 * {@code org.openrewrite.maven.trait.MavenDependency} for the NuGet ecosystem.
 */
@Value
public class NugetDependency implements Trait<Xml.Tag> {
    Cursor cursor;
    String packageId;
    Set<String> versions;

    public static class Matcher extends SimpleTraitMatcher<NugetDependency> {
        private static final XPathMatcher PACKAGE_REFERENCE_MATCHER = new XPathMatcher("/Project/ItemGroup/PackageReference");

        @Override
        protected @Nullable NugetDependency test(Cursor cursor) {
            Object value = cursor.getValue();
            if (!(value instanceof Xml.Tag)) {
                return null;
            }
            Xml.Tag tag = (Xml.Tag) value;
            // `XPathMatcher` is still a bit expensive
            if (!"PackageReference".equals(tag.getName()) || !PACKAGE_REFERENCE_MATCHER.matches(cursor)) {
                return null;
            }
            String include = attribute(tag, "Include");
            if (include == null) {
                return null;
            }
            Xml.Document doc = cursor.firstEnclosing(Xml.Document.class);
            MSBuildProject msbuild = doc == null ? null :
                    doc.getMarkers().findFirst(MSBuildProject.class).orElse(null);
            if (msbuild == null) {
                return null;
            }
            String declaredVersion = attribute(tag, "Version");
            Set<String> versions = new LinkedHashSet<>();
            for (MSBuildProject.TargetFramework tf : msbuild.getTargetFrameworks()) {
                for (MSBuildProject.PackageReference pr : tf.getPackageReferences()) {
                    if (include.equalsIgnoreCase(pr.getInclude()) &&
                            (declaredVersion == null || declaredVersion.equals(pr.getRequestedVersion()))) {
                        String version = pr.getResolvedVersion() != null ? pr.getResolvedVersion() : pr.getRequestedVersion();
                        if (version != null) {
                            versions.add(version);
                        }
                    }
                }
            }
            if (versions.isEmpty() && declaredVersion != null) {
                versions.add(declaredVersion);
            }
            return new NugetDependency(cursor, include, versions);
        }

        private static @Nullable String attribute(Xml.Tag tag, String name) {
            for (Xml.Attribute attribute : tag.getAttributes()) {
                if (name.equals(attribute.getKeyAsString())) {
                    return attribute.getValueAsString();
                }
            }
            return null;
        }
    }
}
