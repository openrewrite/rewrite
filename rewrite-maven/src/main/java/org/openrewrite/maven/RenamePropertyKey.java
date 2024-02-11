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
package org.openrewrite.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class RenamePropertyKey extends Recipe {

    @Option(displayName = "Old key",
            description = "The old name of the property key to be replaced.",
            example = "junit.version")
    String oldKey;

    @Option(displayName = "New key",
            description = "The new property name to use.",
            example = "version.org.junit")
    String newKey;

    @Override
    public String getDisplayName() {
        return "Rename Maven property key";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", oldKey, newKey);
    }

    @Override
    public String getDescription() {
        return "Rename the specified Maven project property key leaving the value unchanged.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            final String oldKeyAsProperty = "${" + oldKey + "}";
            final String newKeyAsProperty = "${" + newKey + "}";

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isPropertyTag() && oldKey.equals(t.getName())) {
                    t = t.withName(newKey);
                    maybeUpdateModel();
                }
                if (t.getChildren().isEmpty()) {
                    Optional<String> value = t.getValue();
                    if (value.isPresent() && value.get().contains(oldKeyAsProperty)) {
                        String newValue = value.get().replace(oldKeyAsProperty, newKeyAsProperty);
                        doAfterVisit(new ChangeTagValueVisitor<>(t, newValue));
                    }
                }
                return t;
            }
        };
    }
}
