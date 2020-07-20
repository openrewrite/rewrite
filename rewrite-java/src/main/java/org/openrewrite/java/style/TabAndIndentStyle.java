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
package org.openrewrite.java.style;

import org.openrewrite.java.JavaStyle;

public class TabAndIndentStyle implements JavaStyle {
    private boolean useTabCharacter = false;
    private int tabSize = 4;
    private int indentSize = 4;
    private int continuationIndent = 8;

    public boolean isUseTabCharacter() {
        return useTabCharacter;
    }

    public void setUseTabCharacter(boolean useTabCharacter) {
        this.useTabCharacter = useTabCharacter;
    }

    public int getTabSize() {
        return tabSize;
    }

    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    public int getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    public int getContinuationIndent() {
        return continuationIndent;
    }

    public void setContinuationIndent(int continuationIndent) {
        this.continuationIndent = continuationIndent;
    }
}
