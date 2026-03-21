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
package org.openrewrite.maven.trait;

/**
 * @deprecated Use specific matchers like {@link MavenDependency.Matcher} or {@link MavenPlugin.Matcher} instead.
 */
@Deprecated
public class Traits {
    private Traits() {
    }

    /**
     * @deprecated Use {@link MavenDependency.Matcher} instead.
     */
    @Deprecated
    public static MavenDependency.Matcher mavenDependency() {
        return new MavenDependency.Matcher();
    }

    /**
     * @deprecated Use {@link MavenPlugin.Matcher} instead.
     */
    @Deprecated
    public static MavenPlugin.Matcher mavenPlugin() {
        return new MavenPlugin.Matcher();
    }
}
