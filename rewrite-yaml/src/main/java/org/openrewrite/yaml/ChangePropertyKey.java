package org.openrewrite.yaml;

import org.openrewrite.Formatting;
import org.openrewrite.yaml.tree.Yaml;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.openrewrite.Formatting.formatFirstPrefix;
import static org.openrewrite.Tree.randomId;

/**
 * When nested YAML mappings are interpreted as dot
 * separated property names, e.g. as Spring Boot
 * interprets application.yml files.
 */
public class ChangePropertyKey extends YamlRefactorVisitor {
    private String property;
    private String toProperty;

    public ChangePropertyKey() {
        setCursoringOn();
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public void setToProperty(String toProperty) {
        this.toProperty = toProperty;
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Yaml visitMappingEntry(Yaml.Mapping.Entry entry) {
        System.out.println(entry);

        Yaml.Mapping.Entry e = refactor(entry, super::visitMappingEntry);

        Deque<Yaml.Mapping.Entry> propertyEntries = getCursor().getPathAsStream()
                .filter(Yaml.Mapping.Entry.class::isInstance)
                .map(Yaml.Mapping.Entry.class::cast)
                .collect(Collectors.toCollection(ArrayDeque::new));

        String property = stream(spliteratorUnknownSize(propertyEntries.descendingIterator(), 0), false)
                .map(e2 -> e2.getKey().getValue())
                .collect(Collectors.joining("."));

        String propertyToTest = this.toProperty;
        if (property.equals(this.property)) {
            Iterator<Yaml.Mapping.Entry> propertyEntriesLeftToRight = propertyEntries.descendingIterator();
            while (propertyEntriesLeftToRight.hasNext()) {
                Yaml.Mapping.Entry propertyEntry = propertyEntriesLeftToRight.next();
                String value = propertyEntry.getKey().getValue();

                if (!propertyToTest.startsWith(value)) {
                    andThen(new InsertSubproperty(
                            propertyEntry,
                            propertyToTest,
                            entry.getValue()
                    ));
                    andThen(new DeleteProperty(propertyEntry));
                    break;
                }

                propertyToTest = propertyToTest.substring(value.length() + 1);
            }
        }

        return e;
    }

    private static class InsertSubproperty extends YamlRefactorVisitor {
        private final Yaml.Mapping.Entry scope;
        private final String subproperty;
        private final Yaml.Block value;

        private InsertSubproperty(Yaml.Mapping.Entry scope, String subproperty, Yaml.Block value) {
            this.scope = scope;
            this.subproperty = subproperty;
            this.value = value;
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping) {
            Yaml.Mapping m = refactor(mapping, super::visitMapping);

            if (m.getEntries().contains(scope)) {
                Formatting newEntryFormatting = scope.getKey().getFormatting();
                if (newEntryFormatting.getPrefix().isEmpty()) {
                    newEntryFormatting = newEntryFormatting.withPrefix("\n");
                }

                m = m.withEntries(Stream.concat(
                        m.getEntries().stream(),
                        Stream.of(
                                new Yaml.Mapping.Entry(randomId(),
                                        new Yaml.Scalar(randomId(), Yaml.Scalar.Style.PLAIN, subproperty,
                                                Formatting.EMPTY),
                                        value.copyPaste(),
                                        newEntryFormatting
                                )
                        )
                ).collect(toList()));
            }

            return m;
        }
    }

    private static class DeleteProperty extends YamlRefactorVisitor {
        private final Yaml.Mapping.Entry scope;

        private DeleteProperty(Yaml.Mapping.Entry scope) {
            this.scope = scope;
            setCursoringOn();
        }

        @Override
        public Yaml visitMapping(Yaml.Mapping mapping) {
            Yaml.Mapping m = refactor(mapping, super::visitMapping);

            boolean changed = false;
            List<Yaml.Mapping.Entry> entries = new ArrayList<>();
            for (Yaml.Mapping.Entry entry : m.getEntries()) {
                if (onlyLeadsToScope(entry)) {
                    changed = true;
                } else {
                    entries.add(entry);
                }
            }

            if (changed) {
                m = m.withEntries(entries);

                if (getCursor().getParentOrThrow().getTree() instanceof Yaml.Document) {
                    Yaml.Document document = getCursor().getParentOrThrow().getTree();
                    if (!document.isExplicit()) {
                        m = m.withEntries(formatFirstPrefix(m.getEntries(), ""));
                    }
                }
            }

            return m;
        }

        private boolean onlyLeadsToScope(Yaml.Mapping.Entry entry) {
            if (scope.equals(entry)) {
                return true;
            }
            if (scope.getValue() instanceof Yaml.Mapping) {
                Yaml.Mapping mapping = (Yaml.Mapping) scope.getValue();
                return mapping.getEntries().size() == 1 && onlyLeadsToScope(mapping.getEntries().iterator().next());
            }
            return false;
        }
    }
}
