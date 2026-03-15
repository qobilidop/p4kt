@file:Suppress("TooManyFunctions")

package p4kt

fun P4Type.toP4(): String =
  when (this) {
    is P4Type.Bit -> "bit<$width>"
    is P4Type.Named -> name
    is P4Type.Bool -> "bool"
    is P4Type.Void -> "void"
    is P4Type.Error -> "error"
    is P4Type.PacketIn -> "packet_in"
    is P4Type.PacketOut -> "packet_out"
  }

fun Direction.toP4(): String =
  when (this) {
    Direction.IN -> "in"
    Direction.OUT -> "out"
    Direction.INOUT -> "inout"
  }

fun P4Param.toP4(): String =
  if (direction != null) {
    "${direction.toP4()} ${type.toP4()} $name"
  } else {
    "${type.toP4()} $name"
  }

fun P4Expr.toP4(): String =
  when (this) {
    is P4Expr.Ref -> name
    is P4Expr.Lit -> "$value"
    is P4Expr.TypedLit -> "${width}w${value}"
    is P4Expr.FieldAccess -> "${expr.toP4()}.$field"
    is P4Expr.BinOp -> {
      val leftStr = if (left is P4Expr.BinOp) "(${left.toP4()})" else left.toP4()
      val rightStr = if (right is P4Expr.BinOp) "(${right.toP4()})" else right.toP4()
      "$leftStr ${op.toP4()} $rightStr"
    }
    is P4Expr.MethodCall -> {
      val argsStr = args.joinToString(", ") { it.toP4() }
      "${expr.toP4()}.$method($argsStr)"
    }
    is P4Expr.ErrorMember -> "error.$name"
  }

fun BinOpKind.toP4(): String =
  when (this) {
    BinOpKind.SUB -> "-"
    BinOpKind.EQ -> "=="
    BinOpKind.NE -> "!="
  }

fun P4Statement.toP4(): String =
  when (this) {
    is P4Statement.Return -> if (expr != null) "return ${expr.toP4()};" else "return;"
    is P4Statement.MethodCall -> {
      val argsStr = args.joinToString(", ") { it.toP4() }
      "${expr.toP4()}.$method($argsStr);"
    }
    is P4Statement.VarDecl ->
      if (init != null) {
        "${type.toP4()} $name = ${init.toP4()};"
      } else {
        "${type.toP4()} $name;"
      }
    is P4Statement.Assign -> "${target.toP4()} = ${value.toP4()};"
    is P4Statement.If -> {
      val thenStr = indentBlock(thenBody, "    ")
      if (elseBody.isEmpty()) {
        "if (${condition.toP4()}) {\n$thenStr\n}"
      } else {
        val elseStr = indentBlock(elseBody, "    ")
        "if (${condition.toP4()}) {\n$thenStr\n} else {\n$elseStr\n}"
      }
    }
    is P4Statement.Verify -> "verify(${condition.toP4()}, ${error.toP4()});"
    is P4Statement.Transition -> "transition $stateName;"
    is P4Statement.TransitionSelect -> {
      val casesStr = cases.joinToString("\n") { (expr, state) -> "    ${expr.toP4()} : $state;" }
      "transition select(${expr.toP4()}) {\n$casesStr\n}"
    }
    is P4Statement.FunctionCall -> {
      val argsStr = args.joinToString(", ") { it.toP4() }
      "$name($argsStr);"
    }
  }

fun indentBlock(statements: List<P4Statement>, indent: String): String =
  statements.joinToString("\n") { stmt -> stmt.toP4().lines().joinToString("\n") { "$indent$it" } }

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
  val bodyStr = indentBlock(body, "    ")
  return "function ${returnType.toP4()} $name($paramStr) {\n$bodyStr\n}"
}

fun P4Const.toP4(): String = "const ${type.toP4()} $name = ${value.toP4()};"

fun P4Action.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  return if (body.isEmpty()) {
    "action $name($paramStr) {\n}"
  } else {
    val bodyStr = indentBlock(body, "    ")
    "action $name($paramStr) {\n$bodyStr\n}"
  }
}

fun MatchKind.toP4(): String =
  when (this) {
    MatchKind.EXACT -> "exact"
    MatchKind.LPM -> "lpm"
    MatchKind.TERNARY -> "ternary"
  }

fun P4LocalVar.toP4(): String = "${type.toP4()} $name;"

fun P4Table.toP4(): String {
  val keyEntries = keys.joinToString(" ") { "${it.expr.toP4()} : ${it.matchKind.toP4()};" }
  val actionsStr = actions.joinToString("\n") { "        $it;" }
  val sizeStr = if (size != null) "\n    size = $size;" else ""
  val constStr = if (isDefaultActionConst) "const " else ""
  return "table $name {\n" +
    "    key = { $keyEntries }\n" +
    "    actions = {\n$actionsStr\n    }$sizeStr\n" +
    "    ${constStr}default_action = $defaultAction;\n}"
}

fun P4Control.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  val declStr =
    declarations.joinToString("\n") { decl -> decl.toP4().lines().joinToString("\n") { "    $it" } }
  val bodyStr = indentBlock(body, "        ")
  val innerParts = mutableListOf<String>()
  if (declStr.isNotEmpty()) innerParts.add(declStr)
  innerParts.add("    apply {\n$bodyStr\n    }")
  return "control $name($paramStr) {\n${innerParts.joinToString("\n")}\n}"
}

fun P4Error.toP4(): String {
  val membersStr = members.joinToString(",\n") { "    $it" }
  return "error {\n$membersStr\n}"
}

fun P4ExternMethod.toP4(externName: String): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  return if (name == externName) {
    "$name($paramStr);"
  } else {
    "${returnType.toP4()} $name($paramStr);"
  }
}

fun P4Extern.toP4(): String {
  val methodsStr = methods.joinToString("\n") { "    ${it.toP4(name)}" }
  return "extern $name {\n$methodsStr\n}"
}

fun P4ExternInstance.toP4(): String = "$typeName() $name;"

fun P4PackageInstance.toP4(): String {
  val argsStr = args.joinToString(", ") { "$it()" }
  return "$typeName($argsStr) $name;"
}

fun P4ParserState.toP4(): String {
  val bodyStr = indentBlock(body, "    ")
  return "state $name {\n$bodyStr\n}"
}

fun P4Parser.toP4(): String {
  val paramStr = params.joinToString(", ") { it.toP4() }
  val innerParts = mutableListOf<String>()
  if (declarations.isNotEmpty()) {
    val declStr =
      declarations.joinToString("\n") { decl ->
        decl.toP4().lines().joinToString("\n") { "    $it" }
      }
    innerParts.add(declStr)
  }
  val startState = states.find { it.name == "start" }
  val otherStates = states.filter { it.name != "start" }
  val orderedStates = listOfNotNull(startState) + otherStates
  for (state in orderedStates) {
    innerParts.add(state.toP4().lines().joinToString("\n") { "    $it" })
  }
  return "parser $name($paramStr) {\n${innerParts.joinToString("\n")}\n}"
}

@Suppress("CyclomaticComplexMethod")
fun P4Declaration.toP4(): String =
  when (this) {
    is P4Function -> (this as P4Function).toP4()
    is P4Typedef -> (this as P4Typedef).toP4()
    is P4Header -> (this as P4Header).toP4()
    is P4Struct -> (this as P4Struct).toP4()
    is P4Const -> (this as P4Const).toP4()
    is P4Action -> (this as P4Action).toP4()
    is P4Table -> (this as P4Table).toP4()
    is P4Control -> (this as P4Control).toP4()
    is P4LocalVar -> (this as P4LocalVar).toP4()
    is P4Error -> (this as P4Error).toP4()
    is P4Extern -> (this as P4Extern).toP4()
    is P4ExternInstance -> (this as P4ExternInstance).toP4()
    is P4Parser -> (this as P4Parser).toP4()
    is P4PackageInstance -> (this as P4PackageInstance).toP4()
  }

fun P4Program.toP4(): String = declarations.joinToString("\n\n") { it.toP4() }
