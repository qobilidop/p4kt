package p4kt

import kotlin.properties.ReadOnlyProperty

// Direction constants

val IN = Direction.IN
val OUT = Direction.OUT
val INOUT = Direction.INOUT

// Match kind constants

val LPM = MatchKind.LPM
val EXACT = MatchKind.EXACT
val TERNARY = MatchKind.TERNARY

// Type factories

fun bit(width: Int) = P4Type.Bit(width)

val bool_ = P4Type.Bool
val void_ = P4Type.Void
val errorType = P4Type.Error
val packet_in = P4Type.PacketIn
val packet_out = P4Type.PacketOut

fun typeName(name: String) = P4Type.Named(name)

// Expression factories and operators

fun ref(name: String) = P4Expr.Ref(name)

fun lit(value: Long) = P4Expr.Lit(value)

fun lit(value: Int) = P4Expr.Lit(value.toLong())

fun lit(width: Int, value: Long) = P4Expr.TypedLit(width, value)

fun lit(width: Int, value: Int) = P4Expr.TypedLit(width, value.toLong())

operator fun P4Expr.minus(other: P4Expr) = P4Expr.BinOp(BinOpKind.SUB, this, other)

infix fun P4Expr.eq(other: P4Expr) = P4Expr.BinOp(BinOpKind.EQ, this, other)

infix fun P4Expr.ne(other: P4Expr) = P4Expr.BinOp(BinOpKind.NE, this, other)

fun P4Expr.call(method: String, vararg args: P4Expr) =
  P4Expr.MethodCall(this, method, args.toList())

fun error_(name: String) = P4Expr.ErrorMember(name)

// Statement builder

open class StatementBuilder {
  protected val body = mutableListOf<P4Statement>()

  fun varDecl(type: P4Type, init: P4Expr? = null) = VarDeclDelegate(body, type, init)

  fun assign(target: P4Expr, value: P4Expr) {
    body.add(P4Statement.Assign(target, value))
  }

  fun if_(condition: P4Expr, block: StatementBuilder.() -> Unit): IfBuilder {
    val thenBuilder = StatementBuilder()
    thenBuilder.block()
    body.add(P4Statement.If(condition, thenBuilder.statements(), emptyList()))
    return IfBuilder(body, body.size - 1)
  }

  fun return_(expr: P4Expr) {
    body.add(P4Statement.Return(expr))
  }

  fun return_() {
    body.add(P4Statement.Return(null))
  }

  fun stmt(statement: P4Statement) {
    body.add(statement)
  }

  fun P4TableRef.apply_() {
    body.add(P4Statement.MethodCall(P4Expr.Ref(name), "apply", emptyList()))
  }

  fun statements() = body.toList()
}

class IfBuilder(private val parentBody: MutableList<P4Statement>, private val index: Int) {
  infix fun else_(block: StatementBuilder.() -> Unit) {
    val elseBuilder = StatementBuilder()
    elseBuilder.block()
    val oldIf = parentBody[index] as P4Statement.If
    parentBody[index] = oldIf.copy(elseBody = elseBuilder.statements())
  }
}

// Struct/header ref classes

class FieldDelegate(
  private val fields: MutableList<P4Field>,
  private val expr: P4Expr,
  private val type: P4Type,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, P4Expr> {
    fields.add(P4Field(property.name, type))
    val fieldAccess = P4Expr.FieldAccess(expr, property.name)
    return ReadOnlyProperty { _, _ -> fieldAccess }
  }
}

class TypedFieldDelegate<T : StructRef>(
  private val fields: MutableList<P4Field>,
  private val expr: P4Expr,
  private val factory: (P4Expr) -> T,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    val instance = factory(P4Expr.FieldAccess(expr, property.name))
    fields.add(P4Field(property.name, P4Type.Named(instance::class.simpleName!!)))
    return ReadOnlyProperty { _, _ -> instance }
  }
}

abstract class StructRef(val expr: P4Expr) {
  val fields = mutableListOf<P4Field>()

  fun field(type: P4Type) = FieldDelegate(fields, expr, type)

  fun field(type: P4TypeReference) = FieldDelegate(fields, expr, type.typeRef)

  fun <T : StructRef> field(factory: (P4Expr) -> T) = TypedFieldDelegate(fields, expr, factory)
}

abstract class HeaderRef(expr: P4Expr) : StructRef(expr)

// Table ref

class P4TableRef(val name: String)
