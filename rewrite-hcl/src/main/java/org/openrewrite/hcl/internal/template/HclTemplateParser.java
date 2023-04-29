/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.hcl.internal.template;

import lombok.RequiredArgsConstructor;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.RandomizeIdVisitor;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class HclTemplateParser {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("#{", "}", null);

    private final Object templateCacheLock = new Object();
    private final HclParser parser = HclParser.builder().build();

    private static final String BODY_STUB = "#{}";
    private static final String EXPRESSION_STUB = "a=#{}";

    private static final Map<String, List<? extends Hcl>> templateCache = new LinkedHashMap<String, List<? extends Hcl>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10_000;
        }
    };

    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;

    public List<BodyContent> parseBodyContent(String template) {
        String stub = substitute(BODY_STUB, template);
        onBeforeParseTemplate.accept(stub);
        return cache(stub, () -> {
            Hcl.ConfigFile cf = compileTemplate(stub);
            return cf.getBody();
        });
    }

    public Expression parseExpression(String template) {
        String stub = substitute(EXPRESSION_STUB, template);
        onBeforeParseTemplate.accept(stub);
        return (Expression) cache(stub, () -> {
            Hcl.ConfigFile cf = compileTemplate(stub);
            return Collections.singletonList(((Hcl.Attribute) cf.getBody().get(0)).getValue());
        }).get(0);
    }

    private Hcl.ConfigFile compileTemplate(String stub) {
        return parser.parse(stub)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as HCL"));
    }

    private String substitute(String stub, String template) {
        String beforeParse = placeholderHelper.replacePlaceholders(stub, k -> template);
        onAfterVariableSubstitution.accept(beforeParse);
        return beforeParse;
    }

    @SuppressWarnings("unchecked")
    private <H extends Hcl> List<H> cache(String stub, Supplier<List<? extends Hcl>> ifAbsent) {
        List<H> hs;
        synchronized (templateCacheLock) {
            hs = (List<H>) templateCache.get(stub);
            if(hs == null) {
                hs = (List<H>) ifAbsent.get();
                templateCache.put(stub, hs);
            }
        }
        return ListUtils.map(hs, j -> (H) new RandomizeIdVisitor<Integer>().visit(j, 0));
    }
}
