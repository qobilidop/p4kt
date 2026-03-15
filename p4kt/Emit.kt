package p4kt

fun P4Type.toP4(): String =
  when (this) {
    is P4Type.Bit -> "bit<$width>"
    is P4Type.Named -> name
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
