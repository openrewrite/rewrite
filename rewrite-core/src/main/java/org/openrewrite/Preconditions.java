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
package org.openrewrite;

/**
 * This class is for rewrite8 migration purpose only
 */
public class Preconditions {

    public static TreeVisitor<?, ExecutionContext> check(Recipe check, TreeVisitor<?, ExecutionContext> v) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }

    public static TreeVisitor<?, ExecutionContext> check(TreeVisitor<?, ExecutionContext> check, TreeVisitor<?, ExecutionContext> v) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }

    public static TreeVisitor<?, ExecutionContext> check(boolean check, TreeVisitor<?, ExecutionContext> v) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }

    @Incubating(since = "8.0.0")
    @SafeVarargs
    public static TreeVisitor<?, ExecutionContext> firstAcceptable(TreeVisitor<?, ExecutionContext>... vs) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }

    public static TreeVisitor<?, ExecutionContext> not(TreeVisitor<?, ExecutionContext> v) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }

    @SafeVarargs
    public static TreeVisitor<?, ExecutionContext> or(TreeVisitor<?, ExecutionContext>... vs) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }

    @SafeVarargs
    public static TreeVisitor<?, ExecutionContext> and(TreeVisitor<?, ExecutionContext>... vs) {
        throw new RuntimeException("Not supported Rewrite8 feature");
    }
}
