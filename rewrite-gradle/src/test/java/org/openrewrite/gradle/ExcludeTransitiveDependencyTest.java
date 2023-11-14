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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

class ExcludeTransitiveDependencyTest implements RewriteTest {

    // TODO: No exclusion. Assert does not add if unnecessary

    // TODO: No exclusion. Assert does not add is already present

    // TODO: Adds exclusion. Assert adds dependency if the dependency matches target.

    // TODO: Adds exclusion to all applicable. Assert adds dependency if a different dependency adds target transitively.
}
