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
package org.openrewrite.java;

import java.util.Collections;
import java.util.Set;

public class ReplaceThreadRunWithThreadStart extends ChangeMethodInvocation {
    public ReplaceThreadRunWithThreadStart() {
        super("java.lang.Thread run()", "start", true);
    }

    @Override
    public String getDisplayName() {
        return "Replace calls to `Thread.run()` with `Thread.start()`";
    }

    @Override
    public String getDescription() {
        return "Replace calls to `Thread.run()` with `Thread.start()`, the former should not be called directly.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-1217");
    }
}
