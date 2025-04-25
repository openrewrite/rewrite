type Expression = Identifier

interface Identifier {
    type: Expression
}

interface FieldAccess {
    name: Expression
}

interface JSSpecificExpression {
}

interface JSFieldAccess extends FieldAccess {
    name: Expression | JSSpecificExpression
}
