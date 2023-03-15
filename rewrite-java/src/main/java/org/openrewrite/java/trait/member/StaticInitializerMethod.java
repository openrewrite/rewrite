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
package org.openrewrite.java.trait.member;

import org.openrewrite.Cursor;
import org.openrewrite.java.trait.util.TraitErrors;
import org.openrewrite.java.trait.util.Validation;

/**
 * A static initializer is a method that contains all static
 * field initializations and static initializer blocks.
 */
public class StaticInitializerMethod extends InitializerMethodBase {
    private StaticInitializerMethod(Cursor cursor) {
        super(cursor);
    }

    @Override
    public String getName() {
        return "<clinit>";
    }

    public static Validation<TraitErrors, StaticInitializerMethod> viewOf(Cursor cursor) {
        return InitializerMethodBase.genericViewOf(cursor, StaticInitializerMethod::new, StaticInitializerMethod.class);
    }
}
