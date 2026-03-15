package p4kt

sealed class P4Type {
  data class Bit(val width: Int) : P4Type()

  data class Named(val name: String) : P4Type()

  data object Bool : P4Type()

  data object Void : P4Type()
}

enum class Direction {
  IN,
  OUT,
  INOUT,
}

data class P4Param(val name: String, val type: P4Type, val direction: Direction)

sealed class P4Expr {
  data class Ref(val name: String) : P4Expr()

  data class Lit(val value: Long) : P4Expr()

  data class TypedLit(val width: Int, val value: Long) : P4Expr()

  data class FieldAccess(val expr: P4Expr, val field: String) : P4Expr()

  data class BinOp(val op: BinOpKind, val left: P4Expr, val right: P4Expr) : P4Expr()
}

enum class BinOpKind {
  SUB,
  EQ,
  NE,
}

sealed class P4Statement {
  data class Return(val expr: P4Expr) : P4Statement()

  data class VarDecl(val name: String, val type: P4Type, val init: P4Expr?) : P4Statement()

  data class Assign(val target: P4Expr, val value: P4Expr) : P4Statement()

  data class If(
    val condition: P4Expr,
    val thenBody: List<P4Statement>,
    val elseBody: List<P4Statement>,
  ) : P4Statement()
}

data class P4Field(val name: String, val type: P4Type)

sealed interface P4Declaration

sealed interface P4TypeReference {
  val typeRef: P4Type.Named
}

data class P4Typedef(val name: String, val type: P4Type) : P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Header(val name: String, val fields: List<P4Field>) : P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Struct(val name: String, val fields: List<P4Field>) : P4Declaration, P4TypeReference {
  override val typeRef
    get() = P4Type.Named(name)
}

data class P4Function(
  val name: String,
  val returnType: P4Type,
  val params: List<P4Param>,
  val body: List<P4Statement>,
) : P4Declaration

data class P4Program(val declarations: List<P4Declaration>)
