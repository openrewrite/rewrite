# Method Quality Metrics

## Per-method complexity and quality measurements

Per-method code quality metrics including cyclomatic complexity, cognitive complexity, nesting depth, ABC metric, and Halstead measures. Use this to identify complex methods that may need refactoring or additional test coverage.

## Data Tables

### Method quality metrics

**File:** [`method-quality-metrics.csv`](method-quality-metrics.csv)

Per-method code quality metrics including cyclomatic complexity, cognitive complexity, nesting depth, Halstead measures, and ABC metric.

| Column | Description |
|--------|-------------|
| Source path | The path to the source file containing the method. |
| Class name | The fully qualified name of the containing class. |
| Method name | The simple name of the method. |
| Method signature | The full method signature including parameter types. |
| Cyclomatic complexity | Number of linearly independent paths through the method. 1-10 low, 11-20 moderate, 21-50 high, 50+ very high. |
| Cognitive complexity | Weighted complexity penalizing nesting depth and flow-breaking structures. |
| Max nesting depth | Maximum depth of nested control structures. |
| Line count | Number of lines in the method body. |
| Parameter count | Number of parameters the method accepts. |
| ABC score | ABC metric magnitude: sqrt(A^2 + B^2 + C^2) where A=assignments, B=branches (calls), C=conditions. |
| Assignments | Number of assignment operations (the A in ABC metric). |
| Branches | Number of method/function calls (the B in ABC metric). |
| Conditions | Number of boolean conditions (the C in ABC metric). |
| Halstead volume | Information content of the method: N * log2(n) where N=total operators+operands, n=distinct operators+operands. |
| Halstead difficulty | Error proneness: (n1/2) * (N2/n2) where n1=distinct operators, N2=total operands, n2=distinct operands. |
| Halstead estimated bugs | Estimated number of delivered bugs: E^(2/3) / 3000 where E = difficulty * volume. |

