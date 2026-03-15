package p4kt

import kotlin.properties.ReadOnlyProperty

val IN = Direction.IN
val OUT = Direction.OUT
val INOUT = Direction.INOUT

fun bit(width: Int) = P4Type.Bit(width)

val bool_ = P4Type.Bool
val void_ = P4Type.Void

fun lit(value: Long) = P4Expr.Lit(value)

fun lit(value: Int) = P4Expr.Lit(value.toLong())

fun lit(width: Int, value: Long) = P4Expr.TypedLit(width, value)

fun lit(width: Int, value: Int) = P4Expr.TypedLit(width, value.toLong())

fun P4Expr.dot(field: String) = P4Expr.FieldAccess(this, field)

operator fun P4Expr.minus(other: P4Expr) = P4Expr.BinOp(BinOpKind.SUB, this, other)

infix fun P4Expr.eq(other: P4Expr) = P4Expr.BinOp(BinOpKind.EQ, this, other)

infix fun P4Expr.ne(other: P4Expr) = P4Expr.BinOp(BinOpKind.NE, this, other)

class FunctionBuilder(private val name: String, private val returnType: P4Type) {
  private val params = mutableListOf<P4Param>()
  private val body = mutableListOf<P4Statement>()

  fun param(type: P4Type, direction: Direction): ReadOnlyProperty<Any?, P4Expr.Ref> {
    var registered = false
    return ReadOnlyProperty { _, property ->
      if (!registered) {
        params.add(P4Param(property.name, type, direction))
        registered = true
      }
      P4Expr.Ref(property.name)
    }
  }

  fun return_(expr: P4Expr) {
    body.add(P4Statement.Return(expr))
  }

  fun build() = P4Function(name, returnType, params, body)
}

fun typeName(name: String) = P4Type.Named(name)

fun p4Typedef(name: String, type: P4Type) = P4Typedef(name, type)

class FieldsBuilder {
  private val fields = mutableListOf<P4Field>()

  fun field(name: String, type: P4Type) {
    fields.add(P4Field(name, type))
  }

  fun field(name: String, type: P4TypeReference) {
    fields.add(P4Field(name, type.typeRef))
  }

  fun build() = fields.toList()
}

fun p4Header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
  val builder = FieldsBuilder()
  builder.block()
  return P4Header(name, builder.build())
}

fun p4Struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
  val builder = FieldsBuilder()
  builder.block()
  return P4Struct(name, builder.build())
}

fun p4Function(name: String, returnType: P4Type, block: FunctionBuilder.() -> Unit): P4Function {
  val builder = FunctionBuilder(name, returnType)
  builder.block()
  return builder.build()
}

class DeclDelegate<T : P4Declaration>(
  private val factory: (String) -> T,
  private val register: (T) -> Unit,
) {
  operator fun provideDelegate(
    thisRef: Any?,
    property: kotlin.reflect.KProperty<*>,
  ): ReadOnlyProperty<Any?, T> {
    val decl = factory(property.name)
    register(decl)
    return ReadOnlyProperty { _, _ -> decl }
  }
}

class ProgramBuilder {
  private val declarations = mutableListOf<P4Declaration>()

  fun typedef(type: P4Type) =
    DeclDelegate<P4Typedef>(
      factory = { name -> p4Typedef(name, type) },
      register = { declarations.add(it) },
    )

  fun header(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Header>(
      factory = { name -> p4Header(name, block) },
      register = { declarations.add(it) },
    )

  fun struct(block: FieldsBuilder.() -> Unit) =
    DeclDelegate<P4Struct>(
      factory = { name -> p4Struct(name, block) },
      register = { declarations.add(it) },
    )

  fun function(returnType: P4Type, block: FunctionBuilder.() -> Unit) =
    DeclDelegate<P4Function>(
      factory = { name -> p4Function(name, returnType, block) },
      register = { declarations.add(it) },
    )

  fun build() = P4Program(declarations.toList())
}

fun p4Program(block: ProgramBuilder.() -> Unit): P4Program {
  val builder = ProgramBuilder()
  builder.block()
  return builder.build()
}
