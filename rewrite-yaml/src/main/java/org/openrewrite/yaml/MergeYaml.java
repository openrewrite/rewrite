package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.yaml.format.AutoFormatVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeYaml extends Recipe {

    @Option(displayName = "Key path",
            description = "XPath expression used to find matching keys.",
            example = "/metadata")
    String key;
    @Option(displayName = "YAML snippet",
            description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.",
            example = "labels: \n\tlabel-one: \"value-one\"")
    @Language("yml")
    String yaml;

    @Override
    public Validated validate() {
        return super.validate()
                .and(Validated.test("yaml", "Must be valid YAML",
                        yaml, y -> {
                            List<Yaml.Documents> docs = new YamlParser().parse(yaml);
                            if (docs.size() == 0) {
                                return false;
                            }
                            Yaml.Documents doc = docs.get(0);
                            if (doc.getDocuments().size() == 0) {
                                return false;
                            }
                            Yaml.Block block = doc.getDocuments().get(0).getBlock();
                            return block instanceof Yaml.Mapping;
                        }));
    }

    @Override
    public String getDisplayName() {
        return "Merge YAML snippet";
    }

    @Override
    public String getDescription() {
        return "Merge a YAML snippet with an existing YAML document";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher matcher = new XPathMatcher(key);
        Yaml.Mapping incoming = (Yaml.Mapping) new YamlParser().parse(yaml).get(0).getDocuments().get(0).getBlock();

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                if (matcher.matches(getCursor())) {
                    doAfterVisit(new MergeYamlVisitor(getCursor().getParentOrThrow().getValue(), incoming));
                    doAfterVisit(new AutoFormatVisitor<>(e));
                }
                return e;
            }
        };
    }

}
