/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;

public class UpgradeVersionProperty extends Recipe {
    @Option(displayName = "Key",
            description = "The name of the property key whose value is to be changed.",
            example = "junit.version")
    private final String key;
    @Option(displayName = "New Version",
            description = "Version to apply to the matching property.",
            example = "4.13")
    private final String newVersion;
    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    private final String versionPattern;
    @Option(displayName = "Add if missing",
            description = "Add the property if it is missing from the pom file.",
            required = false,
            example = "false")
    @Nullable
    private final Boolean addIfMissing;

    @JsonCreator
    public UpgradeVersionProperty(String key, String newVersion, @Nullable String versionPattern, @Nullable Boolean addIfMissing) {
        this.key = key;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.addIfMissing = addIfMissing;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Version Property";
    }

    @Override
    public String getDescription() {
        return "Changes the value of a property, only if the new value is a Semantic Version upgrade from the old value.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Semver.validate(newVersion, versionPattern));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                if (Boolean.TRUE.equals(addIfMissing)) {
                    doAfterVisit(new AddProperty(key, newVersion, true));
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                if (isPropertyTag() && tag.getValue().isPresent()) {
                    if (tag.getName().equals(key)) {
                        final VersionComparator vc = Semver.validate(newVersion, versionPattern).getValue();
                        assert vc != null;
                        vc.upgrade(tag.getValue().get(), Collections.singleton(newVersion))
                                .ifPresent(newVersion -> doAfterVisit(new ChangeTagValueVisitor<>(tag, newVersion)));
                        return tag;
                    }
                }
                return super.visitTag(tag, executionContext);
            }
        };
    }
}
