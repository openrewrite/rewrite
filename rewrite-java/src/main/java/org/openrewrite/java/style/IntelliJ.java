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

public class IntelliJ {
    public static TabsAndIndentsStyle defaultTabsAndIndents() {
        TabsAndIndentsStyle style = new TabsAndIndentsStyle();

        style.setUseTabCharacter(false);
        style.setTabSize(4);
        style.setIndentSize(4);
        style.setContinuationIndent(8);

        return style;
    }

    public static BlankLineStyle defaultBlankLine() {
        BlankLineStyle style = new BlankLineStyle();

        BlankLineStyle.KeepMaximum max = new BlankLineStyle.KeepMaximum();
        max.setInDeclarations(2);
        max.setInCode(2);
        max.setBeforeEndOfBlock(2);
        max.setBetweenHeaderAndPackage(2);

        style.setKeepMaximum(max);

        BlankLineStyle.Minimum min = new BlankLineStyle.Minimum();
        min.setBeforePackage(0);
        min.setAfterPackage(1);
        min.setBeforeImports(3);
        min.setAfterImports(1);
        min.setAroundClass(1);
        min.setAfterClassHeader(0);
        min.setBeforeClassEnd(0);
        min.setAfterAnonymousClassHeader(0);
        min.setAroundFieldInInterface(0);
        min.setAroundField(0);

        style.setMinimum(min);

        return style;
    }
}
