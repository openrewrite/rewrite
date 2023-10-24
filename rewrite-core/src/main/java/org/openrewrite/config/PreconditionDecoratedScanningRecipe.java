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
package org.openrewrite.config;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class PreconditionDecoratedScanningRecipe<T>  extends ScanningRecipe<T> {

    TreeVisitor<?, ExecutionContext> precondition;
    ScanningRecipe<T> delegate;

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public T getInitialValue(ExecutionContext ctx) {
        return delegate.getInitialValue(ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(T acc) {
        return Preconditions.check(precondition, delegate.getScanner(acc));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(T acc) {
        return Preconditions.check(precondition, delegate.getVisitor(acc));
    }
}
