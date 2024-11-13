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
package org.openrewrite.java;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.Reference;
import org.openrewrite.xml.tree.Xml;

@Getter
public class PackageMatcher implements Reference.Renamer, Reference.Matcher {

    @Nullable
    private String targetPackage;

    @Getter
    private final Boolean recursive;

    public PackageMatcher(@Nullable String fieldType) {
        this(fieldType, false);
    }

    public PackageMatcher(@Nullable String fieldType, @Nullable boolean recursive) {
        this.targetPackage = fieldType;
        this.recursive = recursive;
    }

    @Override
    public boolean matchesReference(Reference reference) {
        String recursivePackageNamePrefix = targetPackage + ".";
        if (reference.getValue().equals(targetPackage) || recursive && reference.getValue().startsWith(recursivePackageNamePrefix)) {
            return true;
        }
        return false;
    }

    @Override
    public TreeVisitor<Tree, ExecutionContext> rename(String newValue, boolean recursive) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (StringUtils.isNotEmpty(newValue)) {
                    if (tree instanceof Xml.Attribute) {
                        return ((Xml.Attribute) tree).withValue(((Xml.Attribute) tree).getValue().withValue(getReplacement(((Xml.Attribute) tree).getValueAsString(), targetPackage, newValue)));
                    }
                    if (tree instanceof Xml.Tag) {
                        if (((Xml.Tag) tree).getValue().isPresent()) {
                            return ((Xml.Tag) tree).withValue(getReplacement(((Xml.Tag) tree).getValue().get(), targetPackage, newValue));
                        }
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }

    String getReplacement(String value, String oldValue, String newValue) {
        if (recursive) {
            return value.replace(oldValue, newValue);
        } else if (value.startsWith(oldValue) && Character.isUpperCase(value.charAt(oldValue.length() + 1))) {
            return value.replace(oldValue, newValue);
        }
        return value;
    }

}
