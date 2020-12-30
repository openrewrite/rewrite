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
package org.openrewrite.text;

import org.openrewrite.*;

@AutoConfigure
public class ChangeText extends EvalVisitor<PlainText> {
    private String toText;

    public ChangeText() {
    }

    @Override
    public Validated validate() {
        return Validated.required("toText", toText);
    }

    public void setToText(String toText) {
        this.toText = toText;
    }

    public String getToText() {
        return toText;
    }

    @Override
    public PlainText visitEach(PlainText tree, EvalContext ctx) {
        return tree.withText(toText);
    }
}
