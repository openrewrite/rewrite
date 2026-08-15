package org.openrewrite.scala;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.tree.ParseError;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ScalaSweepTest {
    static final String[] ROOTS = {"/tmp/scala-corpus/cats-effect",
      "/tmp/scala-corpus/scala3/library/src", "/tmp/scala-corpus/scala3/compiler/src"};

    @Test
    void sweep() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String root : ROOTS) {
            Path p = Paths.get(root);
            if (!Files.exists(p)) continue;
            try (Stream<Path> walk = Files.walk(p)) {
                walk.filter(f -> f.toString().endsWith(".scala")).sorted().forEach(files::add);
            }
        }
        Map<String, List<String>> causes = new LinkedHashMap<>();
        int pf = 0;
        ScalaParser parser = ScalaParser.builder().build();
        for (int i = 0; i < files.size(); i += 20) {
            List<Path> chunk = files.subList(i, Math.min(i + 20, files.size()));
            List<Parser.Input> in = chunk.stream().map(f -> new Parser.Input(f, () -> {
                try { return Files.newInputStream(f); } catch (IOException e) { throw new RuntimeException(e); }
            })).collect(Collectors.toList());
            List<SourceFile> res;
            try { res = parser.parseInputs(in, null, new InMemoryExecutionContext(t -> {})).collect(Collectors.toList()); }
            catch (Throwable t) { pf += chunk.size(); continue; }
            for (SourceFile sf : res) {
                if (!(sf instanceof ParseError)) continue;
                pf++;
                String msg = sf.getMarkers().findFirst(ParseExceptionResult.class)
                  .map(ParseExceptionResult::getMessage).orElse("?");
                causes.computeIfAbsent(cause(msg), k -> new ArrayList<>()).add(sf.getSourcePath().toString());
            }
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get("/tmp/pe.txt")))) {
            w.printf("parseErrors=%d%n%n", pf);
            causes.entrySet().stream()
              .sorted(Comparator.<Map.Entry<String,List<String>>>comparingInt(e -> e.getValue().size()).reversed())
              .forEach(e -> { w.printf("%4d  %s%n", e.getValue().size(), e.getKey());
                              e.getValue().stream().limit(2).forEach(f -> w.printf("        %s%n", f)); });
        }
    }

    private static String cause(String m) {
        String head = m.split("\n")[0].trim();
        if (m.contains("CapturesAndResult") || m.contains("did not produce a J.Annotation")) return "capture: annotation/result";
        if (m.contains("Unmapped Scala AST node: New")) return "capture?: unmapped New";
        if (m.contains("PolyFunction")) return "polymorphic function type";
        if (m.contains("Quote") || m.contains("Splice")) return "quote/splice";
        if (!m.contains("is not print idempotent")) return "throw: " + head.replaceAll("/\\S+/", "").replaceAll("\\d+","N");
        List<String> d = new ArrayList<>();
        for (String l : m.split("\n")) {
            if ((l.startsWith("-") || l.startsWith("+")) && !l.startsWith("---") && !l.startsWith("+++")) {
                d.add(l.trim());
                if (d.size() == 2) break;
            }
        }
        String j = String.join(" || ", d);
        if (j.contains("=>")) return "print: arrow =>";
        if (j.contains("using")) return "print: using";
        if (j.contains("end ")) return "print: end marker";
        if (j.contains("inline")) return "print: inline";
        if (j.contains(";")) return "print: semicolon";
        if (j.contains("^")) return "print: capture";
        return "print: other | " + (j.length() > 70 ? j.substring(0, 70) : j);
    }
}
