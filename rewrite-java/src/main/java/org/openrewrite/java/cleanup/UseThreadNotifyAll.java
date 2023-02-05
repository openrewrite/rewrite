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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

/**
 * Recipe to use {@link Thread#notifyAll()} instead of {@link Thread#notify()}
 * <p>
 * {@link Thread#notify()} and {@link Thread#notifyAll()} both wake up sleeping threads, but {@link Thread#notify()} only rouses one,
 * while {@link Thread#notifyAll()} rouses all of them.
 * Since {@link Thread#notify()} might not wake up the right thread, {@link Thread#notifyAll()} should be used instead.
 *
 * @see <a href="https://rules.sonarsource.com/java/quickfix/RSPEC-2446"></a>
 * @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/THI02-J.+Notify+all+waiting+threads+rather+than+a+single+thread"></a>
 */
public class UseThreadNotifyAll extends Recipe {

    private static final MethodMatcher OBJECT_NOTIFY = new MethodMatcher("java.lang.Object notify()");

    @Override
    public String getDisplayName() {
        return "Replaces `Thread#notify()` with `Thread#notifyAll()`";
    }

    @Override
    public String getDescription() {
        return "`Thread#notifyAll()` should be used instead of `Thread#notify()` as the latter might not wake up the right thread.";
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
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(OBJECT_NOTIFY);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation invocation = super.visitMethodInvocation(method, context);

                if (OBJECT_NOTIFY.matches(method)) {
                    return invocation
                            .withName(invocation.getName().withSimpleName("notifyAll"));
                }

                return invocation;
            }
        };

    }
}
