/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openrewrite.scheduling;

import org.openrewrite.RecipeScheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class DirectScheduler implements RecipeScheduler {

    private static final DirectScheduler COMMON = new DirectScheduler();

    public static RecipeScheduler common() {
        return COMMON;
    }

    @Override
    public <T> CompletableFuture<T> schedule(Callable<T> fn) {
        try {
            return CompletableFuture.completedFuture(fn.call());
        } catch (Exception e) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

}
