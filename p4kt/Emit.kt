package p4kt

fun P4Type.toP4(): String =
  when (this) {
    is P4Type.Bit -> "bit<$width>"
    is P4Type.Named -> name
    is P4Type.Bool -> "bool"
    is P4Type.Void -> "void"
  }

fun Direction.toP4(): String =
  when (this) {
    Direction.IN -> "in"
    Direction.OUT -> "out"
    Direction.INOUT -> "inout"
  }

fun P4Param.toP4(): String = "${direction.toP4()} ${type.toP4()} $name"

fun P4Expr.toP4(): String =
  when (this) {
    is P4Expr.Ref -> name
    is P4Expr.Lit -> "$value"
    is P4Expr.TypedLit -> "${width}w${value}"
    is P4Expr.FieldAccess -> "${expr.toP4()}.$field"
    is P4Expr.BinOp -> "(${left.toP4()} ${op.toP4()} ${right.toP4()})"
  }

fun BinOpKind.toP4(): String =
  when (this) {
    BinOpKind.SUB -> "-"
    BinOpKind.EQ -> "=="
    BinOpKind.NE -> "!="
  }

fun P4Statement.toP4(): String =
  when (this) {
    is P4Statement.Return -> "return ${expr.toP4()};"
  }

fun P4Field.toP4(): String = "${type.toP4()} $name;"

fun P4Typedef.toP4(): String = "typedef ${type.toP4()} $name;"

fun P4Header.toP4(): String {
  val fieldsStr = fields.joinToString("\n") { "    ${it.toP4()}" }
  return "header $name {\n$fieldsStr\n}"
}

fun P4Struct.toP4(): String {
  val fieldsStr = fields.joinToString("\n") { "    ${it.toP4()}" }
  return "struct $name {\n$fieldsStr\n}"
}

fun P4Function.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  val bodyStr = body.joinToString("\n") { "    ${it.toP4()}" }
  return "function ${returnType.toP4()} $name($paramStr) {\n$bodyStr\n}"
}

fun P4Declaration.toP4(): String =
  when (this) {
    is P4Function -> (this as P4Function).toP4()
    is P4Typedef -> (this as P4Typedef).toP4()
    is P4Header -> (this as P4Header).toP4()
    is P4Struct -> (this as P4Struct).toP4()
  }

fun P4Program.toP4(): String = declarations.joinToString("\n\n") { it.toP4() }
