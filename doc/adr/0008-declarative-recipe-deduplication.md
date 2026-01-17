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
For example, if boolean simplification were implemented with a recipe per identity in boolean algebra, full simplification of a complex expression could be achieved by running each of the boolean identity recipes after the other.
Even in cases like these it would be better for the boolean identity recipes to instead be visitors which were internally looped over inside a single boolean simplification recipe. 
The `org.openrewrite.Repeat` utilities can be used for recipe-internal iteration of visitors.

## Decision

We will implement view-based deduplication of declarative recipes in `DeclarativeRecipe.getRecipeList()`:

### Implementation approach

1. **Initialization remains unchanged**: Recipes are initialized normally with all children (including duplicates)
   - This avoids breaking existing caching and reuse of recipe instances during initialization

2. **Deduplication on-demand**: When `getRecipeList()` is called, deduplication is applied as a view:
   - Create a fresh `seen` set to track declarative recipe names
   - Recursively walk the tree via `deduplicateRecursively()`
   - Skip duplicate declarative recipes (second and subsequent occurrences)
   - Non-declarative recipes are never deduplicated
     - Duplicated non-declarative recipes is a non-problem right now.
     - Provides a way to opt-in to duplication if we really need to

3. **Immutable copying**: The `withDeduplication(Set<String> seen)` method creates new recipe instances:
   - Creates a new `DeclarativeRecipe` with the same metadata
   - Recursively deduplicates children using the shared `seen` set
   - Sets the deduplicated recipe list on the copy
   - Returns the immutable copy without affecting the original

### Other approaches not selected

There were several ideas for how to resolve the performance problems of large duplicated composites which were considered.

- "Highlander" precondition - manually opt-in a recipe to "there can be only one" status via precondition
  - Not clear how to actually implement this precondition.  
- boolean "allowDuplication" or "denyDuplication" flags on a declarative recipe
  - General distaste for boolean flags
- Using the already-existing "causesAnotherCycle" flag to opt-in or opt-out of allowing duplicates
  - The concept of "cycles" - running all the recipes in a loop - is in effect largely the same thing as duplicating the recipe list multiple times
  - The concept of cycles is going away, already off in the CLI, so this would end up an ugly and confusing vestige 

Since we could come up with no concrete examples of where we would want to opt-in to duplication we ultimately decided 
it would be cleanest and easiest to make no-duplication the default.

### Key characteristics

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

### Implementation details

- Location: `org.openrewrite.config.DeclarativeRecipe`
- Key methods:
  - `getRecipeList()`: Entry point that creates `seen` set and calls `deduplicateRecursively()`
  - `deduplicateRecursively()`: Walks tree and applies deduplication logic
  - `withDeduplication()`: Creates immutable copies with deduplicated children
- Test coverage: `DeclarativeRecipeTest.recipeInstanceIntegrityAfterDeduplication()` validates the approach
