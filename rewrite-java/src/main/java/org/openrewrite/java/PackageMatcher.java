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
import org.openrewrite.trait.Reference;

@Getter
public class PackageMatcher implements Reference.Matcher {

    private final @Nullable String targetPackage;

    @Getter
    private final Boolean recursive;

    public PackageMatcher(@Nullable String targetPackage) {
        this(targetPackage, false);
    }

    public PackageMatcher(@Nullable String targetPackage, boolean recursive) {
        this.targetPackage = targetPackage;
        this.recursive = recursive;
    }

    @Override
    public boolean matchesReference(Reference reference) {
        if (reference.getKind().equals(Reference.Kind.TYPE) || reference.getKind().equals(Reference.Kind.PACKAGE)) {
            String recursivePackageNamePrefix = targetPackage + ".";
            if (reference.getValue().equals(targetPackage) || recursive && reference.getValue().startsWith(recursivePackageNamePrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Reference.Renamer renameTo(String newName) {
        return oldName -> getReplacement(oldName, targetPackage, newName);
    }

    String getReplacement(String value, @Nullable String oldValue, String newValue) {
        if (oldValue != null) {
            if (recursive) {
                return value.replace(oldValue, newValue);
            } else if (value.startsWith(oldValue) && Character.isUpperCase(value.charAt(oldValue.length() + 1))) {
                return value.replace(oldValue, newValue);
            }
        }
        return value;
    }
}
