package p4kt

fun P4Type.toP4(): String =
  when (this) {
    is P4Type.Bit -> "bit<$width>"
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

fun P4Function.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  val bodyStr = body.joinToString("\n") { "    ${it.toP4()}" }
  return "function ${returnType.toP4()} $name($paramStr) {\n$bodyStr\n}"
}
