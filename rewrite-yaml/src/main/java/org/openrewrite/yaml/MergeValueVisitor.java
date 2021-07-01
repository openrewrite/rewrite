package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeValueVisitor<B extends Yaml, Y extends Yaml> extends YamlIsoVisitor<ExecutionContext> {

    String xpath;
    BiPredicate<B, ExecutionContext> shouldMergeFunction;
    BiFunction<B, ExecutionContext, Y> mergeFunction;

    XPathMatcher matcher;

    public MergeValueVisitor(String xpath,
                             BiPredicate<B, ExecutionContext> shouldMergeFunction,
                             BiFunction<B, ExecutionContext, Y> mergeFunction) {
        this.xpath = xpath;
        this.shouldMergeFunction = shouldMergeFunction;
        this.mergeFunction = mergeFunction;

        this.matcher = new XPathMatcher(xpath);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext context) {
        if (matcher.matches(getCursor()) && shouldMergeFunction.test((B) entry.getValue(), context)) {
            return entry.withValue((Yaml.Block) new MergeYamlVisitor(entry.getValue(), mergeFunction.apply((B) entry.getValue(), context))
                    .visit(entry.getValue(), context, getCursor()));
        }
        return super.visitMappingEntry(entry, context);
    }

}
