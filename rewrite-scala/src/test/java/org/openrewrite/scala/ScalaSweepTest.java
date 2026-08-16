package org.openrewrite.scala;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.internal.WhitespaceValidationService;
import org.openrewrite.tree.ParseError;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
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
        int ok = 0, pf = 0, ws = 0;
        Map<String,Integer> peCause = new LinkedHashMap<>(), wsCause = new LinkedHashMap<>();
        List<String> wsSamples = new ArrayList<>();
        for (int i = 0; i < files.size(); i += 20) {
            List<Path> chunk = files.subList(i, Math.min(i + 20, files.size()));
            List<Parser.Input> in = chunk.stream().map(f -> new Parser.Input(f, () -> {
                try { return Files.newInputStream(f); } catch (IOException e) { throw new RuntimeException(e); }
            })).collect(Collectors.toList());
            ExecutionContext ctx = new InMemoryExecutionContext(t -> {});
            List<SourceFile> res;
            try { res = ScalaParser.builder().build().parseInputs(in, null, ctx).collect(Collectors.toList()); }
            catch (Throwable t) { pf += chunk.size(); continue; }
            for (SourceFile sf : res) {
                if (sf instanceof ParseError) {
                    pf++;
                    peCause.merge(peCause(sf.getMarkers().findFirst(ParseExceptionResult.class)
                      .map(ParseExceptionResult::getMessage).orElse("?")), 1, Integer::sum);
                    continue;
                }
                ok++;
                try {
                    WhitespaceValidationService s = sf.service(WhitespaceValidationService.class);
                    SourceFile v = (SourceFile) s.getVisitor().visit(sf, ctx);
                    if (v != null && v != sf) {
                        ws++;
                        Matcher mm = Pattern.compile("~~\\(non-whitespace\\)~~>(.{0,25})", Pattern.DOTALL).matcher(v.printAll());
                        String c = mm.find() ? wsCause(mm.group(1)) : "?";
                        wsCause.merge(c, 1, Integer::sum);
                        if (c.equals("type ascription") && wsSamples.size() < 10) wsSamples.add(sf.getSourcePath() + "  |" + mm.group(1).replace("\n","\\n") + "|");
                    }
                } catch (UnsupportedOperationException ignored) {}
            }
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get("/tmp/both-fresh.txt")))) {
            w.printf("files=%d  parseErrors=%d  unsound=%d  sound=%d%n%n== parse errors ==%n", files.size(), pf, ws, ok - ws);
            peCause.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
              .limit(14).forEach(e -> w.printf("%4d  %s%n", e.getValue(), e.getKey()));
            w.printf("%n== samples ==%n");
            wsSamples.forEach(x2 -> w.printf("   %s%n", x2));
            w.printf("%n== unsound ==%n");
            wsCause.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
              .limit(10).forEach(e -> w.printf("%4d  %s%n", e.getValue(), e.getKey()));
        }
    }

    private static String peCause(String m) {
        if (m.contains("CapturesAndResult") || m.contains("did not produce a J.Annotation")) return "capture checking";
        if (m.contains("PolyFunction")) return "polymorphic function type";
        if (m.contains("Quote") || m.contains("Splice")) return "quote/splice";
        if (!m.contains("is not print idempotent")) return "throw: " + m.split("\n")[0].replaceAll("/\\S+/","").replaceAll("\\d+","N").trim();
        List<String> d = new ArrayList<>();
        for (String l : m.split("\n")) if ((l.startsWith("-")||l.startsWith("+")) && !l.startsWith("---") && !l.startsWith("+++")) { d.add(l.trim()); if (d.size()==2) break; }
        String j = String.join(" || ", d);
        if (j.contains("=>")) return "print: arrow | " + (j.length() > 70 ? j.substring(0, 70) : j);
        if (j.contains("using")) return "print: using";
        if (j.contains("end ")) return "print: end marker";
        if (j.contains(";")) return "print: semicolon";
        return "print: other | " + (j.length()>60 ? j.substring(0,60) : j);
    }

    private static String wsCause(String s) {
        String t = s.trim();
        if (t.startsWith("using")) return "using args";
        if (t.startsWith("inline")) return "inline modifier";
        if (t.startsWith("end ")) return "end marker";
        if (t.startsWith("=")) return "method body =";
        if (t.contains("=>")) return "self type / arrow";
        if (t.startsWith("^")) return "capture set";
        if (t.startsWith(":")) return "type ascription";
        if (t.startsWith(",")) return "comma";
        if (t.startsWith("private")||t.startsWith("protected")) return "access modifier";
        return "other: " + (t.length()>16 ? t.substring(0,16) : t);
    }
}
