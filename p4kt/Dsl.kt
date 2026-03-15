package p4kt

import kotlin.properties.ReadOnlyProperty

val IN = Direction.IN
val OUT = Direction.OUT
val INOUT = Direction.INOUT

fun bit(width: Int) = P4Type.Bit(width)

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

class ProgramBuilder {
  private val declarations = mutableListOf<P4Declaration>()

  fun typedef(name: String, type: P4Type): P4Typedef {
    val decl = p4Typedef(name, type)
    declarations.add(decl)
    return decl
  }

  fun header(name: String, block: FieldsBuilder.() -> Unit): P4Header {
    val decl = p4Header(name, block)
    declarations.add(decl)
    return decl
  }

  fun struct(name: String, block: FieldsBuilder.() -> Unit): P4Struct {
    val decl = p4Struct(name, block)
    declarations.add(decl)
    return decl
  }

  fun function(name: String, returnType: P4Type, block: FunctionBuilder.() -> Unit): P4Function {
    val decl = p4Function(name, returnType, block)
    declarations.add(decl)
    return decl
  }

  fun build() = P4Program(declarations.toList())
}

fun p4Program(block: ProgramBuilder.() -> Unit): P4Program {
  val builder = ProgramBuilder()
  builder.block()
  return builder.build()
}
