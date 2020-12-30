/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EvalContext {
    Collection<SourceFile> generate = new ArrayList<>();
    Map<String, Object> baggage = new HashMap<>();

    public EvalContext generate(SourceFile sourceFile) {
        this.generate.add(sourceFile);
        return this;
    }

    @Nullable
    public <T> T findBaggage(String key) {
        //noinspection unchecked
        return (T) baggage.get(key);
    }

    @Nullable
    public <T> T removeBaggage(String key) {
        //noinspection unchecked
        return (T) baggage.remove(key);
    }

    public void next() {
        generate.clear();
    }
}
