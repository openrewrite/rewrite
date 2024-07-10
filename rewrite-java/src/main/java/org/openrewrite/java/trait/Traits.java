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
package org.openrewrite.java.trait;

import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.MethodMatcher;

public class Traits {

    public static Literal.Matcher literal() {
        return new Literal.Matcher();
    }

    public static VariableAccess.Matcher variableAccess() {
        return new VariableAccess.Matcher();
    }

    public static MethodAccess.Matcher methodAccess(MethodMatcher matcher) {
        return new MethodAccess.Matcher(matcher);
    }

    public static MethodAccess.Matcher methodAccess(String signature) {
        return new MethodAccess.Matcher(signature);
    }

    public static Annotated.Matcher annotated(AnnotationMatcher matcher) {
        return new Annotated.Matcher(matcher);
    }

    public static Annotated.Matcher annotated(String signature) {
        return new Annotated.Matcher(signature);
    }
}
