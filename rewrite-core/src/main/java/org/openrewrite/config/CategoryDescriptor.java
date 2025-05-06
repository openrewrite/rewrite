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
package org.openrewrite.config;

import lombok.Value;
import lombok.With;
import org.intellij.lang.annotations.Language;
import org.openrewrite.NlsRewrite;

import java.util.Set;

@Value
@With
public class CategoryDescriptor {
    public static final int LOWEST_PRECEDENCE = -1;
    public static final int DEFAULT_PRECEDENCE = 0;
    public static final int HIGHEST_PRECEDENCE = Integer.MAX_VALUE;

    @Language("markdown")
    @NlsRewrite.DisplayName
    String displayName;

    /**
     * Represents the part of the package under the root packages collected in {@link #rootPackages}
     * If a category contains the packages "org.openrewrite.java" and "com.company.java" this will become "java"
     */
    String subPackageName;

    /**
     * Backwards compatibility method for retrieving the packageName
     * which has been changed to only the subPackage name ("org.openrewrite.java" becomes "java")
     *
     * @return the sub-package name of this category descriptor.
     * @deprecated Use {@link #getSubPackageName()} instead
     */
    @Deprecated
    public String getPackageName() {
        return subPackageName;
    }

    @Language("markdown")
    @NlsRewrite.Description
    String description;

    Set<String> tags;
    boolean root;

    /**
     * Defines the sort order for category descriptors of the same {@link #subPackageName}. The description, tags, and root values of the highest
     * priority category descriptor for a given package name will be used.
     * <p/>
     * Lower values have higher priority. The default value is {@link #LOWEST_PRECEDENCE}, indicating the lowest priority
     * (effectively deferring to any other specified priority value).
     **/
    int priority;

    boolean synthetic;

    Set<String> rootPackages;
}
