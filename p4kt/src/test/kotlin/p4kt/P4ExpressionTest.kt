package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("TooManyFunctions")
class P4ExpressionTest {
  @Test
  fun boolType() {
    assertEquals("bool", P4.bool_.toP4())
  }

  @Test
  fun voidType() {
    assertEquals("void", P4.void_.toP4())
  }

  @Test
  fun untypedLiteral() {
    assertEquals("42", P4.lit(42).toP4())
  }

  @Test
  fun hexLiteral() {
    assertEquals("2048", P4.lit(0x0800).toP4())
  }

  @Test
  fun typedLiteral() {
    assertEquals("4w4", P4.lit(4, 4).toP4())
  }

  @Test
  fun typedLiteralZero() {
    assertEquals("16w0", P4.lit(16, 0).toP4())
  }

  @Test
  fun fieldAccess() {
    val headers = P4Expr.Ref("headers")
    assertEquals("headers.ip", P4Expr.FieldAccess(headers, "ip").toP4())
  }

  @Test
  fun chainedFieldAccess() {
    val headers = P4Expr.Ref("headers")
    assertEquals(
      "headers.ip.ttl",
      P4Expr.FieldAccess(P4Expr.FieldAccess(headers, "ip"), "ttl").toP4(),
    )
  }

  @Test
  fun subtraction() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")
    assertEquals("a - b", (a - b).toP4())
  }

  @Test
  fun equality() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")
    assertEquals("a == b", (a eq b).toP4())
  }

  @Test
  fun inequality() {
    val a = P4Expr.Ref("a")
    val b = P4Expr.Ref("b")
    assertEquals("a != b", (a ne b).toP4())
  }

  @Test
  fun nestedExpression() {
    val headers = P4Expr.Ref("headers")
    val ttl = P4Expr.FieldAccess(P4Expr.FieldAccess(headers, "ip"), "ttl")
    assertEquals("headers.ip.ttl - 1", (ttl - P4.lit(1)).toP4())
  }

  // Task 1: Method call expression

  @Test
  fun methodCallExpression() {
    val expr = P4Expr.MethodCall(P4Expr.Ref("ck"), "get", emptyList())
    assertEquals("ck.get()", expr.toP4())
  }

  @Test
  fun methodCallExpressionWithArgs() {
    val expr = P4Expr.MethodCall(P4Expr.Ref("b"), "extract", listOf(P4Expr.Ref("p")))
    assertEquals("b.extract(p)", expr.toP4())
  }

  @Test
  fun methodCallExpressionDsl() {
    val ck = P4.ref("ck")
    assertEquals("ck.get()", ck.call("get").toP4())
  }

  // Task 2: ErrorMember expression

  @Test
  fun errorMemberExpression() {
    val expr = P4Expr.ErrorMember("IPv4IncorrectVersion")
    assertEquals("error.IPv4IncorrectVersion", expr.toP4())
  }

  @Test
  fun errorMemberDsl() {
    assertEquals("error.NoError", P4.error_("NoError").toP4())
  }

  // Task 3: New types

  @Test
  fun errorType() {
    assertEquals("error", P4Type.Error.toP4())
  }

  @Test
  fun namedTypeRendering() {
    assertEquals("packet_in", P4Type.Named("packet_in").toP4())
    assertEquals("packet_out", P4Type.Named("packet_out").toP4())
  }
}
