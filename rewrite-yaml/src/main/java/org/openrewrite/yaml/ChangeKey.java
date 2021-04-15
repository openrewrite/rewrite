package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeKey extends Recipe {
    @Option(displayName = "Old key path",
            description = "An XPath expression to locate a YAML entry.",
            example = "subjects/kind")
    String oldKeyPath;

    @Option(displayName = "New key",
            example = "kind")
    String newKey;

    @Override
    public String getDisplayName() {
        return "Change key";
    }

    @Override
    public String getDescription() {
        return "Change a YAML mapping entry key leaving the value intact.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher xPathMatcher = new XPathMatcher(oldKeyPath);
        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext context) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, context);
                if (xPathMatcher.matches(getCursor())) {
                    e = e.withKey(e.getKey().withValue(newKey));
                }
                return e;
            }
        };
    }
}
