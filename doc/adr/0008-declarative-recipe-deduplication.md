# 8. Declarative recipe deduplication

Date: 2026-01-16

## Status

Accepted

## Context

Declarative recipes defined in YAML can include other declarative recipes, creating recipe hierarchies. When multiple recipes include the same sub-recipe, duplicates can occur in the recipe tree. For example, if recipe A includes both B and C, and both B and C include D, then D would be executed multiple times when running A.
This can represent a major performance problem when large composites include each other. 
In one case this more than doubled the size of an already-large composite to over 11,000 steps.

Recipes are generally expected to be idempotent, making the required changes in a single pass and then making no further changes.
It generally expected that having two copies of the same recipe in a recipe list is wasteful as the second copy is expected to make no changes.

It *is* possible to want a given recipe to be able to respond to the changes made by another recipe within the same run. 
For example, if boolean simplification were implemented with a recipe per identity in boolean algebra, full simplification of a complex expression could be achieved by running


## Decision

We will implement view-based deduplication of declarative recipes in `DeclarativeRecipe.getRecipeList()`:

### Implementation approach

1. **Initialization remains unchanged**: Recipes are initialized normally with all children (including duplicates)
   - This allows for caching and reuse of recipe instances during initialization

2. **Deduplication on-demand**: When `getRecipeList()` is called, deduplication is applied as a view:
   - Create a fresh `seen` set to track declarative recipe names
   - Recursively walk the tree via `deduplicateRecursively()`
   - Skip duplicate declarative recipes (second and subsequent occurrences)
   - Non-declarative recipes are never deduplicated
     - The original problem we set out to solve is multi-inclusion of large composites. 
     - There are (uncommon) scenarios where you might want to repeat a recipe multiple times so it can respond to changes made by another recipe in the list
     - Determining the equality of non-declarative recipe instances is not necessarily trivial

3. **Immutable copying**: The `withDeduplication(Set<String> seen)` method creates new recipe instances:
   - Creates a new `DeclarativeRecipe` with the same metadata
   - Recursively deduplicates children using the shared `seen` set
   - Sets the deduplicated recipe list on the copy
   - Returns the immutable copy without affecting the original

### Key characteristics

- **Global deduplication**: The `seen` set is shared across the entire tree, so D appears only once even if referenced at multiple levels
- **Non-mutating**: Original recipe instances are never modified
- **Independent usage**: Calling `getRecipeList()` on C directly (without A) still shows D because C's original state is unchanged
- **First-wins**: The first occurrence of a duplicate recipe in tree traversal order is kept

### Example

When recipe A includes B and C, and both B and C include D:

```
Calling A.getRecipeList():
  Recipe A
  ├─ Recipe B
  │  └─ Recipe D (first occurrence - included)
  └─ Recipe C (deduplicated copy without D)

Calling C.getRecipeList() independently:
  Recipe C
  └─ Recipe D (original instance unchanged)
```

## Consequences

### Benefits

1. **Recipe independence**: Each recipe maintains its original state and works correctly when loaded independently or as part of a hierarchy
2. **No side effects**: Calling `getRecipeList()` doesn't permanently affect any recipe instances
3. **Correctness**: Shared recipe instances are never corrupted by deduplication
4. **Performance**: Duplicate recipes are not executed multiple times within a recipe hierarchy
5. **Memory efficiency**: Instance reuse during initialization (same recipe object referenced multiple times)
6. **On-demand**: Deduplication computed only when needed, not stored permanently

### Trade-offs

1. **Copy overhead**: Creating deduplicated copies on each `getRecipeList()` call has a small runtime cost
2. **Memory**: Temporary recipe copies are created during deduplication (though they're short-lived)
3. **Complexity**: The implementation requires recursive copying logic in `withDeduplication()`

### Implementation details

- Location: `DeclarativeRecipe.java` lines 340-407
- Key methods:
  - `getRecipeList()`: Entry point that creates `seen` set and calls `deduplicateRecursively()`
  - `deduplicateRecursively()`: Walks tree and applies deduplication logic
  - `withDeduplication()`: Creates immutable copies with deduplicated children
- Cycle detection (lines 151-156): Prevents infinite loops during initialization
- Test coverage: `DeclarativeRecipeTest.recipeInstanceIntegrityAfterDeduplication()` validates the approach

### Migration impact

This change is backward compatible:
- Recipes that don't have duplicates are unaffected
- Recipes with duplicates now execute correctly (once instead of multiple times)
- No changes required to existing YAML recipe definitions
- Tested against rewrite-spring to ensure no regressions
