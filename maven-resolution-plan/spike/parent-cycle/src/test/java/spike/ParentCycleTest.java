package spike;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spike.support.CycleAwareModelResolver;
import spike.support.FilesystemModelResolver;
import spike.support.Fixtures;
import spike.support.Poms;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static spike.support.CycleAwareModelResolver.Strategy.KEEP_GAV;
import static spike.support.CycleAwareModelResolver.Strategy.MUTATE_ID;

/**
 * Empirically characterizes Maven 3.9.16 {@code DefaultModelBuilder}'s FATAL parent-cycle behavior and tests whether a
 * cycle-aware {@link org.apache.maven.model.resolution.ModelResolver} can degrade gracefully instead of aborting.
 *
 * <p>The cycle check lives in {@code DefaultModelBuilder} lines 288/358: a {@code LinkedHashSet<String> parentIds}
 * that throws a FATAL {@code ModelBuildingException} the instant a model id repeats. Crucially the id is derived from
 * the CHILD's {@code <parent>} element (readParentExternally lines 1095-1096), not from the resolved parent bytes.
 */
class ParentCycleTest {

    // ---------------------------------------------------------------------------------------------
    // E1 — baseline: two-pom repo-lookup cycle (A.parent=B, B.parent=A), plain resolver.
    // ---------------------------------------------------------------------------------------------
    @Test
    void e1_repoLookupCycle_isFatal(@TempDir File tmp) {
        Poms poms = new Poms(tmp);
        File a = poms.write("com.example", "a", "1",
                Fixtures.withRepoParent("com.example", "a", "1", "pom", "com.example", "b", "1", ""));
        poms.write("com.example", "b", "1",
                Fixtures.withRepoParent("com.example", "b", "1", "pom", "com.example", "a", "1", ""));

        ModelBuildingException ex = assertThrows(ModelBuildingException.class,
                () -> Fixtures.build(a, new FilesystemModelResolver(poms.byGav)));

        dumpException("E1 repo-lookup cycle", ex);
        assertTrue(hasFatalCycleProblem(ex), "expected a FATAL 'parents form a cycle' problem");
    }

    // ---------------------------------------------------------------------------------------------
    // E2 — relativePath cycle: A and B in sibling dirs referencing each other via relativePath.
    // ---------------------------------------------------------------------------------------------
    @Test
    void e2_relativePathCycle_isFatal(@TempDir File tmp) {
        Poms poms = new Poms(tmp);
        File a = poms.writeAt(new File(tmp, "a/pom.xml"), "com.example", "a", "1",
                Fixtures.withRelativeParent("com.example", "a", "1", "pom",
                        "com.example", "b", "1", "../b/pom.xml", ""));
        poms.writeAt(new File(tmp, "b/pom.xml"), "com.example", "b", "1",
                Fixtures.withRelativeParent("com.example", "b", "1", "pom",
                        "com.example", "a", "1", "../a/pom.xml", ""));

        ModelBuildingException ex = assertThrows(ModelBuildingException.class,
                () -> Fixtures.build(a, new FilesystemModelResolver(poms.byGav)));

        dumpException("E2 relativePath cycle", ex);
        assertTrue(hasFatalCycleProblem(ex), "expected a FATAL 'parents form a cycle' problem");
    }

    // ---------------------------------------------------------------------------------------------
    // E3(a) — stub keeps the revisited GAV. PREDICTED DEAD: parentIds check still fires.
    // ---------------------------------------------------------------------------------------------
    @Test
    void e3a_stubKeepingRevisitedGav_stillFatal(@TempDir File tmp) {
        Poms poms = twoPomCycleWithInheritance(tmp);
        File a = poms.byGav.get("com.example:app:1");
        File stubDir = new File(tmp, "stubs");

        CycleAwareModelResolver resolver =
                new CycleAwareModelResolver(poms.byGav, stubDir, KEEP_GAV, "com.example:app:1");

        ModelBuildingException ex = assertThrows(ModelBuildingException.class,
                () -> Fixtures.build(a, resolver));

        System.out.println("E3a resolver log: " + resolver.log);
        dumpException("E3a stub-keeps-GAV", ex);
        assertTrue(hasFatalCycleProblem(ex),
                "DISPROVEN: a stub under the revisited GAV still trips parentIds (id comes from the child's <parent>)");
    }

    // ---------------------------------------------------------------------------------------------
    // E3(b) — stub under a synthesized non-colliding GAV (mutate the passed Parent). PREDICTED: works.
    // ---------------------------------------------------------------------------------------------
    @Test
    void e3b_stubUnderSynthesizedGav_succeedsAndInheritanceSurvives(@TempDir File tmp) throws Exception {
        Poms poms = twoPomCycleWithInheritance(tmp);
        File a = poms.byGav.get("com.example:app:1");
        File stubDir = new File(tmp, "stubs");

        CycleAwareModelResolver resolver =
                new CycleAwareModelResolver(poms.byGav, stubDir, MUTATE_ID, "com.example:app:1");

        ModelBuildingResult result = Fixtures.build(a, resolver);
        Model effective = result.getEffectiveModel();

        System.out.println("E3b resolver log: " + resolver.log);
        System.out.println("E3b modelIds: " + result.getModelIds());

        assertNotNull(effective, "cycle broken: an effective model is produced");
        assertEquals("com.example", effective.getGroupId());
        assertEquals("app", effective.getArtifactId());
        assertEquals("1", effective.getVersion());

        // The real parent (base) still contributed its dependencyManagement through the (broken) chain.
        Dependency managed = findManaged(effective, "com.example", "lib");
        assertNotNull(managed, "parent-contributed managed dependency survived the cycle break");
        assertEquals("9.9.9", managed.getVersion());
    }

    // ---------------------------------------------------------------------------------------------
    // E4 — partial-result fallback: catch the exception and extract whatever interim model exists.
    // ---------------------------------------------------------------------------------------------
    @Test
    void e4_partialResultFromException(@TempDir File tmp) {
        Poms poms = twoPomCycleWithInheritance(tmp);
        File a = poms.byGav.get("com.example:app:1");

        ModelBuildingException ex = assertThrows(ModelBuildingException.class,
                () -> Fixtures.build(a, new FilesystemModelResolver(poms.byGav)));

        ModelBuildingResult result = ex.getResult();
        System.out.println("E4 getResult() != null: " + (result != null));
        System.out.println("E4 getModelIds(): " + (result == null ? "n/a" : result.getModelIds()));
        System.out.println("E4 getEffectiveModel(): " + (result == null ? "n/a" : result.getEffectiveModel()));
        System.out.println("E4 ex.getModel(): " + ex.getModel());

        assertNotNull(result, "ModelBuildingException carries an interim result");
        // Effective model is null: the throw happens mid-lineage-walk, before setEffectiveModel().
        assertNull(result.getEffectiveModel(), "no effective model is available on a cycle abort");

        // The ONLY salvage is the ROOT RAW model (newModelBuildingException seeds modelIds with the root id).
        assertFalse(result.getModelIds().isEmpty(), "root model id is present");
        String rootId = result.getModelIds().get(0);
        Model rootRaw = result.getRawModel(rootId);
        System.out.println("E4 salvaged rootId=" + rootId + " rawModel=" + rootRaw);
        assertNotNull(rootRaw, "root RAW model is salvageable");
        assertEquals("app", rootRaw.getArtifactId());

        // But it is the UN-INHERITED root: the parent's dependencyManagement is NOT present.
        assertNull(rootRaw.getDependencyManagement(),
                "salvaged raw root has NO parent inheritance (poorer than rewrite's ancestry-up-to-repeat model)");
    }

    // ---------------------------------------------------------------------------------------------
    // E5 — self-parent (A.parent=A). SURPRISE: this is NOT caught by the parentIds cycle check. The
    // raw-model validator (DefaultModelValidator.validateRawModel, UNCONDITIONAL — not gated by
    // validation level) rejects a <parent> whose GA equals the project's GA, aborting at the root
    // readModel BEFORE the resolver is ever consulted. So the stub strategy alone cannot help.
    // ---------------------------------------------------------------------------------------------
    private static String selfParentPom() {
        return Fixtures.withRepoParent("com.example", "app", "1", "jar",
                "com.example", "app", "1", Fixtures.managed("com.example", "lib", "9.9.9"));
    }

    @Test
    void e5a_selfParent_plainResolver_fatalViaRawValidationNotCycleCheck(@TempDir File tmp) {
        Poms poms = new Poms(tmp);
        File a = poms.write("com.example", "app", "1", selfParentPom());

        ModelBuildingException ex = assertThrows(ModelBuildingException.class,
                () -> Fixtures.build(a, new FilesystemModelResolver(poms.byGav)));
        dumpException("E5a self-parent plain", ex);

        assertFalse(hasFatalCycleProblem(ex), "self-parent does NOT reach the parentIds cycle check");
        assertTrue(hasFatalProblemContaining(ex, "cannot have the same groupId:artifactId"),
                "self-parent is rejected earlier, by raw-model validation");
    }

    @Test
    void e5b_selfParent_mutateIdStub_cannotHelp_resolverNeverConsulted(@TempDir File tmp) {
        Poms poms = new Poms(tmp);
        File a = poms.write("com.example", "app", "1", selfParentPom());
        CycleAwareModelResolver resolver =
                new CycleAwareModelResolver(poms.byGav, new File(tmp, "stubs"), MUTATE_ID, "com.example:app:1");

        ModelBuildingException ex = assertThrows(ModelBuildingException.class, () -> Fixtures.build(a, resolver));
        System.out.println("E5b resolver log (expected empty): " + resolver.log);

        assertTrue(resolver.log.isEmpty(), "resolver is never called: raw validation aborts first");
        assertTrue(hasFatalProblemContaining(ex, "cannot have the same groupId:artifactId"));
    }

    // Viable strategy A: pre-supply the raw model with the self-parent intact. Setting request.rawModel skips the
    // root readModel/validateRawModel, so the self-parent reaches the MUTATE_ID resolver and is broken like any cycle.
    // This UNIFIES self-parent with the multi-pom cycle path under a single mechanism.
    @Test
    void e5c_selfParent_preSuppliedRawModel_plusMutateIdStub_succeeds(@TempDir File tmp) throws Exception {
        Poms poms = new Poms(tmp);
        File a = poms.write("com.example", "app", "1", selfParentPom());
        CycleAwareModelResolver resolver =
                new CycleAwareModelResolver(poms.byGav, new File(tmp, "stubs"), MUTATE_ID, "com.example:app:1");

        ModelBuildingResult result = Fixtures.buildFromRawModel(a, Fixtures.readRaw(a), resolver);
        Model effective = result.getEffectiveModel();
        System.out.println("E5c resolver log: " + resolver.log);
        System.out.println("E5c modelIds: " + result.getModelIds());

        assertNotNull(effective);
        assertEquals("app", effective.getArtifactId());
        assertEquals("1", effective.getVersion());
        assertNotNull(findManaged(effective, "com.example", "lib"), "self-declared managed dependency survives");
    }

    // Viable strategy B: pre-supply the raw model with the degenerate self-parent stripped. Simplest; no resolver
    // interaction needed for the parent at all.
    @Test
    void e5d_selfParent_preSuppliedRawModel_parentStripped_succeeds(@TempDir File tmp) throws Exception {
        Poms poms = new Poms(tmp);
        File a = poms.write("com.example", "app", "1", selfParentPom());
        Model raw = Fixtures.readRaw(a);
        raw.setParent(null); // drop the self-parent before building

        ModelBuildingResult result =
                Fixtures.buildFromRawModel(a, raw, new FilesystemModelResolver(poms.byGav));
        Model effective = result.getEffectiveModel();
        System.out.println("E5d modelIds: " + result.getModelIds());

        assertNotNull(effective);
        assertEquals("app", effective.getArtifactId());
        assertNotNull(findManaged(effective, "com.example", "lib"), "self-declared managed dependency survives");
    }

    // ---- fixtures / helpers ----

    /** app.parent=base, base.parent=app; the real parent (base) declares a managed dependency to prove inheritance. */
    private static Poms twoPomCycleWithInheritance(File tmp) {
        Poms poms = new Poms(tmp);
        poms.write("com.example", "app", "1",
                Fixtures.withRepoParent("com.example", "app", "1", "jar", "com.example", "base", "1", ""));
        poms.write("com.example", "base", "1",
                Fixtures.withRepoParent("com.example", "base", "1", "pom", "com.example", "app", "1",
                        Fixtures.managed("com.example", "lib", "9.9.9")));
        return poms;
    }

    private static boolean hasFatalCycleProblem(ModelBuildingException ex) {
        return hasFatalProblemContaining(ex, "form a cycle");
    }

    private static boolean hasFatalProblemContaining(ModelBuildingException ex, String needle) {
        for (ModelProblem p : ex.getProblems()) {
            if (p.getSeverity() == ModelProblem.Severity.FATAL && p.getMessage().contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Dependency findManaged(Model model, String g, String a) {
        if (model.getDependencyManagement() == null) {
            return null;
        }
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            if (g.equals(d.getGroupId()) && a.equals(d.getArtifactId())) {
                return d;
            }
        }
        return null;
    }

    private static void dumpException(String label, ModelBuildingException ex) {
        System.out.println("=== " + label + " ===");
        System.out.println("  exception: " + ex.getClass().getName());
        System.out.println("  message  : " + ex.getMessage());
        for (ModelProblem p : ex.getProblems()) {
            System.out.println("  problem  : [" + p.getSeverity() + "] " + p.getMessage());
        }
    }
}
