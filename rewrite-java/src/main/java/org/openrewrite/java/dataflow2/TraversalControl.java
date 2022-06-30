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
package org.openrewrite.java.dataflow2;

import org.openrewrite.Incubating;

@Incubating(since = "7.25.0")
public class TraversalControl<S extends ProgramState<?>> {

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static <S extends ProgramState<?>> TraversalControl<S> noop() {
        return new TraversalControl<>();
    }
}
