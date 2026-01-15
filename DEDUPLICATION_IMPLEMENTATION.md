# Declarative Recipe Deduplication Implementation

## Overview

This branch implements deduplication logic for declarative recipes to optimize loading and execution performance. The implementation uses **LOCAL deduplication** where each recipe maintains its own scope.

## Implementation Details

### Key Changes in DeclarativeRecipe.java

1. **Cycle Detection** (lines 143-147, 169-173)
   - Tracks recipes currently being initialized via `initializingRecipes` set
   - Throws `RecipeIntrospectionException` if a cycle is detected
   - Provides clear error messages showing the cycle path

2. **Local Deduplication** (lines 149-152, 175-178)
   - Tracks declarative recipes seen within a recipe's direct children via `seenDeclarativeRecipeNames`
   - Skips duplicate declarative recipes within the same parent
   - Each child recipe gets its own `seenDeclarativeRecipeNames` set (line 199)

3. **Initialization Guard** (lines 128-131)
   - Returns early if recipe is already initialized (uninitialized list is empty)
   - Prevents re-initialization of recipes

### Local vs Global Deduplication

**Current Approach (LOCAL):**
```
Recipe A
├─ Recipe B
│  └─ Recipe D
└─ Recipe C
   └─ Recipe D
```

Both B and C include D. Each maintains its own complete scope.

**Alternative Approach (GLOBAL) - NOT implemented:**
```
Recipe A
├─ Recipe B
│  └─ Recipe D
└─ Recipe C
```

D would only appear under B (first occurrence), not under C.

### Why Local Deduplication?

The implementation evolved from GLOBAL to LOCAL deduplication through these commits:
- `d48eb912c` - Initial implementation (GLOBAL approach)
- `4c6001a0d` - Improve deduplication (refined GLOBAL approach)
- `4fcefab54` - WIP (switched to LOCAL approach to fix independence issue)

**Key Problem with Global Deduplication:**
When recipe A includes B and C, and both include D:
- A initializes B → B includes D (D is in B's uninitialized list, gets processed)
- A initializes C → D is skipped because it was seen in parent scope
- C's uninitialized list is cleared, but D was skipped
- Later loading C separately → C has no D! ❌

**Solution with Local Deduplication:**
- A initializes B → B gets its own scope, includes D
- A initializes C → C gets its own scope, also includes D
- Each recipe is initialized once with its complete scope
- Later loading C separately → C still has D ✅

**Benefits:**
1. **Independence**: Each recipe maintains its own complete recipe list
2. **Reusability**: Recipe C can be used independently and still includes D
3. **No Side Effects**: Loading recipe A doesn't permanently affect C's recipe list
4. **Instance Reuse**: While D appears in multiple places, it's the same instance (initialized once)
5. **Correct Behavior**: Test `recipeLoadedSeparatelyStillHasAllChildren` validates this

## Test Coverage

### DeclarativeRecipeTest.java
- ✅ Self-referencing recipe cycle detection
- ✅ Mutually recursive recipes cycle detection
- ✅ Deeper cyclic references (A → B → C → A) detection
- ✅ Preconditions functionality
- ✅ Validation of uninitialized recipes

### YamlResourceLoaderTest.java
- ✅ Non-declarative recipes are NOT deduplicated (different configurations allowed)
- ✅ Duplicate declarative recipes are deduplicated within a recipe's direct children
- ✅ Deduplication works across hierarchy (A includes B and C, both include D)
- ✅ Deduplication works across multiple YAML resource loaders
- ✅ Recipe loaded separately retains all children

## Test Results

All tests passing:
- ✅ DeclarativeRecipeTest: All tests pass
- ✅ YamlResourceLoaderTest: All tests pass
- ✅ Full rewrite-core test suite: All tests pass

## Example Scenarios

### Scenario 1: Direct Duplicates (Deduplicated)
```yaml
type: specs.openrewrite.org/v1beta/recipe
name: test.A
recipeList:
  - test.D
  - test.D  # This duplicate is removed
  - test.D  # This duplicate is removed
```
Result: A includes D only once

### Scenario 2: Hierarchical Dependencies (Both Maintained)
```yaml
type: specs.openrewrite.org/v1beta/recipe
name: test.B
recipeList:
  - test.D
---
type: specs.openrewrite.org/v1beta/recipe
name: test.C
recipeList:
  - test.D
---
type: specs.openrewrite.org/v1beta/recipe
name: test.A
recipeList:
  - test.B
  - test.C
```
Result:
- A includes B and C
- B includes D
- C includes D (maintained, not deduplicated from A's perspective)

### Scenario 3: Independent Usage
```yaml
# After loading A (which includes both B and C)
# Loading C separately still shows D in its recipe list
```

## Performance Benefits

1. **Initialization**: Each declarative recipe is initialized only once
2. **Memory**: Duplicate references point to the same instance
3. **Validation**: Early validation of cycles prevents runtime issues
4. **Execution**: Recipes within the same parent aren't duplicated

## Remaining Work

Based on the "WIP" commit status, potential areas for further work:

1. ✅ Cycle detection - COMPLETE
2. ✅ Local deduplication - COMPLETE
3. ✅ Test coverage - COMPLETE
4. ⚠️ Performance benchmarking - Could be added
5. ⚠️ Documentation in code - Could be improved

## Merge Status

- ✅ Main branch merged into no-duplicate-declaratives (Jan 15, 2026)
- ✅ No merge conflicts
- ✅ All tests passing after merge
