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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class UseObjectNotifyAll extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replaces `Object.notify()` with `Object.notifyAll()`";
    }

    @Override
    public String getDescription() {
        return "`Object.notifyAll()` and `Object.notify()` both wake up sleeping threads, but `Object.notify()` only rouses one while `Object.notifyAll()` rouses all of them. " +
               "Since `Object.notify()` might not wake up the right thread, `Object.notifyAll()` should be used instead. " +
               "See [this](https://wiki.sei.cmu.edu/confluence/display/java/THI02-J.+Notify+all+waiting+threads+rather+than+a+single+thread) for more information.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2446");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher objectNotify = new MethodMatcher("java.lang.Object notify()");
        return Preconditions.check(new UsesMethod<>(objectNotify), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation m = super.visitMethodInvocation(method, context);
                return objectNotify.matches(method) ? m
                        .withName(m.getName().withSimpleName("notifyAll")) : m;
            }
        });
    }
}
