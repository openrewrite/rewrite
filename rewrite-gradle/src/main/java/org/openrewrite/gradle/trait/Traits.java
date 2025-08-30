/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import org.openrewrite.java.InlineMe;

/**
 * @deprecated Use specific matchers like {@link GradleDependency.Matcher} or {@link JvmTestSuite.Matcher} instead.
 */
@Deprecated
public class Traits {
    private Traits() {
    }

    /**
     * @deprecated Use {@link GradleDependency.Matcher} instead.
     */
    @Deprecated
    @InlineMe(replacement = "new GradleDependency.Matcher()", imports = "org.openrewrite.gradle.GradleDependency")
    public static GradleDependency.Matcher gradleDependency() {
        return new GradleDependency.Matcher();
    }

    /**
     * @deprecated Use {@link JvmTestSuite.Matcher} instead.
     */
    @Deprecated
    @InlineMe(replacement = "new JvmTestSuite.Matcher()", imports = "org.openrewrite.gradle.JvmTestSuite")
    public static JvmTestSuite.Matcher jvmTestSuite() {
        return new JvmTestSuite.Matcher();
    }
}
