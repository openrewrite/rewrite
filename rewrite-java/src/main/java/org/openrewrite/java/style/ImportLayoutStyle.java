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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.java.JavaStyle;
import org.openrewrite.java.OrderImports;

import java.util.List;
import java.util.Map;

public class ImportLayoutStyle implements JavaStyle {
    private Map<String, Object> layout;

    public OrderImports orderImports() {
        OrderImports.Layout.Builder builder = OrderImports.Layout.builder(
                (Integer) layout.getOrDefault("classCountToUseStarImport", 5),
                (Integer) layout.getOrDefault("nameCountToUseStarImport", 3));

        //noinspection unchecked
        for (String block : (List<String>) layout.get("blocks")) {
            block = block.trim();
            if (block.equals("<blank line>")) {
                builder = builder.blankLine();
            } else if (block.startsWith("import ")) {
                block = block.substring("import ".length());
                boolean statik = false;
                if (block.startsWith("static")) {
                    statik = true;
                    block = block.substring("static ".length());
                }
                if (block.equals("all other imports")) {
                    builder = statik ?
                            builder.importStaticAllOthers() :
                            builder.importAllOthers();
                } else {
                    builder = statik ?
                            builder.staticImportPackage(block) :
                            builder.importPackage(block);
                }
            }
        }

        OrderImports.Layout layout = builder.build();

        OrderImports orderImports = new OrderImports();
        orderImports.setLayout(layout);

        return orderImports;
    }

    public Map<String, Object> getLayout() {
        return this.layout;
    }

    @JsonProperty("layout")
    public void setLayout(Map<String, Object> layout) {
        this.layout = layout;
    }
}
