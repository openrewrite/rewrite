/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.marker.ExecSyntax;
import org.openrewrite.python.marker.LegacyNotEqual;
import org.openrewrite.python.marker.PrintSyntax;
import org.openrewrite.python.marker.Quoted;
import org.openrewrite.python.marker.RaiseTuple;
import org.openrewrite.python.marker.TupleExceptClause;
import org.openrewrite.python.rpc.PythonRewriteRpc;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.request.Print;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

/**
 * The Python 2 half of {@code PythonPrinterParityTest}: what the Python 2 parser produces must
 * print the same from {@link PythonPrinter} as it does from the Python process.
 */
class Python2PrinterParityTest {

    private static final List<String> SYNTAX = List.of(
      "print 42\n",
      "print 1, 2, 3,\n",
      "print 1 , 2 , 3 ,\n",
      "print >>sys.stderr, 'error'\n",
      "print >>  f , x ,\n",
      "exec code\n",
      "exec  code in globals_dict\n",
      "exec code in globals_dict , locals_dict\n",
      "x = `42`\ny = `foo + bar`\n",
      "x = 100L\ny = 0777\nz = ur\"both\"\n",
      "#!/usr/bin/env python2\nprint 'hi'\n",
      "import os; print os\nprint x; y = 1\n",
      "raise E, v\n",
      "raise E , v , tb\n",
      "raise(E), v\n",
      "x = a <> b\nif a<>b and c <>  d: pass\n",
      // the print statement keeps a newer interpreter from reading the clause as a tuple of types
      "print 1\ntry:\n    f()\nexcept E, e:\n    g(e)\n",
      "print 1\ntry:\n    f()\nexcept (A, B) , e:\n    g(e)\n",
      "\"\"\"doc\"\"\"\nprint 1\nx\n(a)\na[0]\n",
      """
        import os
        from os.path import join, dirname


        def greet(name, greeting="hello"):
            \"""Says hello.\"""
            if name:
                print greeting, name
            else:
                print >> sys.stderr, "missing name"
            return greeting + name


        class Worker(Base):
            def run(self, items):
                results = []
                for item in items:
                    try:
                        results.append(self.process(item))
                    except ValueError, e:
                        self.log(e)
                    finally:
                        exec "done = True" in self.scope
                return results
        """
    );

    @Test
    void python2Syntax() {
        Set<Class<?>> markers = new HashSet<>();
        SoftAssertions.assertSoftly(softly -> {
            List<SourceFile> parsed = parse(SYNTAX);
            for (int i = 0; i < parsed.size(); i++) {
                assertThat(parsed.get(i)).as(SYNTAX.get(i)).isInstanceOf(Py.CompilationUnit.class);
                new PythonVisitor<Integer>() {
                    @Override
                    public J preVisit(J tree, Integer p) {
                        // a marker the Python process has no codec for arrives as null
                        assertThat(tree.getMarkers().getMarkers()).doesNotContainNull();
                        for (Marker marker : tree.getMarkers().getMarkers()) {
                            markers.add(marker.getClass());
                        }
                        return tree;
                    }
                }.visit(parsed.get(i), 0);
                String printed = parsed.get(i).printAll();
                softly.assertThat(printed).isEqualTo(SYNTAX.get(i));
                softly.assertThat(printed).isEqualTo(PythonRewriteRpc.getOrStart().print(parsed.get(i)));
            }
        });
        assertThat(markers).contains(PrintSyntax.class, ExecSyntax.class, Quoted.class,
          RaiseTuple.class, TupleExceptClause.class, LegacyNotEqual.class);
    }

    @Test
    void markersBuiltOnTheHost() {
        Set<Class<?>> built = new HashSet<>();
        SoftAssertions.assertSoftly(softly -> {
            List<SourceFile> parsed = parse(SYNTAX);
            for (int i = 0; i < parsed.size(); i++) {
                // under a new id a marker is sent whole, where one the parser attached is only referred back to
                SourceFile rebuilt = (SourceFile) new PythonVisitor<Integer>() {
                    @Override
                    public <M extends Marker> M visitMarker(Marker marker, Integer p) {
                        built.add(marker.getClass());
                        return marker.withId(randomId());
                    }
                }.visitNonNull(parsed.get(i), 0);
                softly.assertThat(PythonRewriteRpc.getOrStart().print(rebuilt)).isEqualTo(SYNTAX.get(i));
            }
        });
        assertThat(built).contains(PrintSyntax.class, ExecSyntax.class, Quoted.class,
          RaiseTuple.class, TupleExceptClause.class, LegacyNotEqual.class);
    }

    @Test
    void subtrees() {
        SoftAssertions.assertSoftly(softly -> {
            for (SourceFile parsed : parse(SYNTAX)) {
                new PythonVisitor<Integer>() {
                    @Override
                    public J preVisit(J tree, Integer p) {
                        if (!(tree instanceof SourceFile)) {
                            Cursor parent = getCursor().getParentOrThrow();
                            softly.assertThat(tree.print(parent))
                              .as("%s in %s", tree.getClass().getName(), parsed.printAll())
                              .isEqualTo(PythonRewriteRpc.getOrStart().print(tree, parent));
                        }
                        return tree;
                    }
                }.visit(parsed, 0);
            }
        });
    }

    @Test
    void spellingsLostFromStoredTrees() {
        List<String> sources = List.of(
          "raise E, v, tb\n",
          "raise(E), v\n",
          "x = a <> b\n",
          "print 1\ntry:\n    f()\nexcept E, e:\n    g(e)\n",
          "print 1\ntry:\n    f()\nexcept (A, B), e:\n    g(e)\n"
        );
        List<PrintOutputCapture.MarkerPrinter> markerPrinters = List.of(
          PrintOutputCapture.MarkerPrinter.DEFAULT, PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY,
          PrintOutputCapture.MarkerPrinter.FENCED, PrintOutputCapture.MarkerPrinter.SANITIZED);

        SoftAssertions.assertSoftly(softly -> {
            List<SourceFile> parsed = parse(sources);
            for (int i = 0; i < parsed.size(); i++) {
                assertThat(parsed.get(i)).as(sources.get(i)).isInstanceOf(Py.CompilationUnit.class);
                SourceFile stored = asStoredWithoutCodecs(parsed.get(i));
                for (PrintOutputCapture.MarkerPrinter markerPrinter : markerPrinters) {
                    softly.assertThat(stored.printAll(new PrintOutputCapture<>(0, markerPrinter))).isEqualTo(sources.get(i));
                    softly.assertThat(PythonRewriteRpc.getOrStart().print(stored, Print.MarkerPrinter.from(markerPrinter)))
                      .isEqualTo(sources.get(i));
                }

                // a recipe marks the tree that holds the null, which stays where it is and is sent again
                SourceFile found = (SourceFile) new PythonVisitor<Integer>() {
                    @Override
                    public J preVisit(J tree, Integer p) {
                        return tree.getMarkers().getMarkers().contains(null) ? SearchResult.found(tree) : tree;
                    }
                }.visitNonNull(stored, 0);
                softly.assertThat(found.printAll()).isNotEqualTo(sources.get(i));
                softly.assertThat(found.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                  .as("the legacy spelling of a marked statement").isEqualTo(sources.get(i));
                softly.assertThat(PythonRewriteRpc.getOrStart().print(found)).isEqualTo(found.printAll());

                SourceFile unmarked = (SourceFile) new PythonVisitor<Integer>() {
                    @Override
                    public J preVisit(J tree, Integer p) {
                        return tree.withMarkers(tree.getMarkers().removeByType(SearchResult.class));
                    }
                }.visitNonNull(found, 0);
                softly.assertThat(unmarked.printAll()).isEqualTo(sources.get(i));
            }
        });
    }

    /**
     * An LST stored while the Python 2 spellings had no codec holds null where the parser attached one.
     */
    private static SourceFile asStoredWithoutCodecs(SourceFile cu) {
        Set<Class<?>> dropped = new HashSet<>();
        SourceFile stored = (SourceFile) new PythonVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                List<Marker> markers = new ArrayList<>(tree.getMarkers().getMarkers());
                boolean lost = false;
                for (int i = 0; i < markers.size(); i++) {
                    Marker marker = markers.get(i);
                    if (marker instanceof RaiseTuple || marker instanceof TupleExceptClause || marker instanceof LegacyNotEqual) {
                        dropped.add(marker.getClass());
                        markers.set(i, null);
                        lost = true;
                    }
                }
                return lost ? tree.withMarkers(tree.getMarkers().withMarkers(markers)) : tree;
            }
        }.visitNonNull(cu, 0);
        assertThat(dropped).hasSize(1);
        return stored;
    }

    private static List<SourceFile> parse(List<String> sources) {
        return PythonParser.builder().build().parse(sources.toArray(new String[0])).collect(toList());
    }
}
