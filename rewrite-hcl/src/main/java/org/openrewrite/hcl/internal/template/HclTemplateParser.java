package org.openrewrite.hcl.internal.template;

import lombok.RequiredArgsConstructor;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.RandomizeIdVisitor;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class HclTemplateParser {
    private static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("#{", "}", null);

    private final Object templateCacheLock = new Object();
    private final HclParser parser = new HclParser();

    private static final Map<String, List<? extends Hcl>> templateCache = new LinkedHashMap<String, List<? extends Hcl>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > 10_000;
        }
    };

    private final Consumer<String> onAfterVariableSubstitution;
    private final Consumer<String> onBeforeParseTemplate;

    public List<BodyContent> parseBodyContent(String template) {
        String stub = substitute("#{}", template);
        onBeforeParseTemplate.accept(stub);
        return cache(stub, () -> {
            Hcl.ConfigFile cf = compileTemplate(stub);
            return cf.getBody().getContents();
        });
    }

    private Hcl.ConfigFile compileTemplate(String stub) {
        return parser.parse(stub).get(0);
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
