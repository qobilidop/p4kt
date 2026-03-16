package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals

class P4ControlTest {
  @Test
  fun stmtAddsArbitraryStatement() {
    val a =
      P4.action("Test") { stmt(P4Statement.MethodCall(P4Expr.Ref("t"), "apply", emptyList())) }

    assertEquals(
      """
          action Test() {
              t.apply();
          }
      """
        .trimIndent(),
      a.toP4(),
    )
  }

  @Test
  fun tableDsl() {
    val dropAction = P4.action("Drop_action") {}
    val setNhop = P4.action("Set_nhop") {}

    val table =
      P4.table("ipv4_match") {
        key(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), P4MatchKindRef("lpm"))
        actions(dropAction, setNhop)
        size(1024)
        defaultAction(dropAction)
      }

    assertEquals("Drop_action", table.actions[0])
    assertEquals("Set_nhop", table.actions[1])
    assertEquals(1024, table.size)
    assertEquals("Drop_action", table.defaultAction)
  }

  @Test
  fun controlDsl() {
    class OutControl(base: P4Expr) : P4.StructRef(base) {
      val outputPort by field(P4.bit(4))
    }

    @Suppress("VariableNaming") val IPv4Address = P4Typedef("IPv4Address", P4.bit(32))

    val ctrl =
      P4.control("TopPipe") {
        val outCtrl by param(::OutControl, P4.OUT)

        val Drop_action by action { assign(outCtrl.outputPort, P4.ref("DROP_PORT")) }

        @Suppress("UnusedPrivateProperty", "VariableNaming") val nextHop by varDecl(IPv4Address)

        val ipv4_match by table {
          key(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), P4MatchKindRef("lpm"))
          actions(Drop_action)
          size(1024)
          defaultAction(Drop_action)
        }

        apply {
          ipv4_match.apply_()
          if_(outCtrl.outputPort eq P4.ref("DROP_PORT")) { return_() }
        }
      }

    assertEquals("TopPipe", ctrl.name)
    assertEquals(1, ctrl.params.size)
    assertEquals(3, ctrl.declarations.size) // action + varDecl + table
    assertEquals(2, ctrl.body.size) // method call + if
  }

  @Test
  fun controlInProgram() {
    class OutControl(base: P4Expr) : P4.StructRef(base) {
      val outputPort by field(P4.bit(4))
    }

    val program =
      P4.program {
        struct(::OutControl)

        @Suppress("UnusedPrivateProperty")
        val TopPipe by control {
          val outCtrl by param(::OutControl, P4.OUT)

          @Suppress("UnusedPrivateProperty")
          val Drop by action { assign(outCtrl.outputPort, P4.lit(4, 0xF)) }

          apply { if_(outCtrl.outputPort eq P4.lit(4, 0xF)) { return_() } }
        }
      }

    assertEquals(
      """
          struct OutControl {
              bit<4> outputPort;
          }

          control TopPipe(out OutControl outCtrl) {
              action Drop() {
                  outCtrl.outputPort = 4w15;
              }
              apply {
                  if (outCtrl.outputPort == 4w15) {
                      return;
                  }
              }
          }
      """
        .trimIndent(),
      program.toP4(),
    )
  }

  @Test
  fun callDslStatement() {
    val action = P4.action("test") { call(P4Expr.Ref("ck"), "clear") }
    assertEquals(
      """
          action test() {
              ck.clear();
          }
      """
        .trimIndent(),
      action.toP4(),
    )
  }

  @Test
  fun callDslStatementWithArgs() {
    val action = P4.action("test") { call(P4Expr.Ref("b"), "extract", P4Expr.Ref("p")) }
    assertEquals(
      """
          action test() {
              b.extract(p);
          }
      """
        .trimIndent(),
      action.toP4(),
    )
  }

  @Test
  fun methodCallStatement() {
    val stmt = P4Statement.MethodCall(P4Expr.Ref("ipv4_match"), "apply", emptyList())
    assertEquals("ipv4_match.apply();", stmt.toP4())
  }

  @Test
  fun tableRendering() {
    val table =
      P4Table(
        name = "ipv4_match",
        keys =
          listOf(
            P4KeyEntry(P4Expr.FieldAccess(P4Expr.Ref("headers"), "dstAddr"), P4MatchKindRef("lpm"))
          ),
        actions = listOf("Drop_action", "Set_nhop"),
        size = 1024,
        defaultAction = "Drop_action",
        isDefaultActionConst = false,
      )

    assertEquals(
      """
          table ipv4_match {
              key = { headers.dstAddr : lpm; }
              actions = {
                  Drop_action;
                  Set_nhop;
              }
              size = 1024;
              default_action = Drop_action;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun tableWithConstDefaultAction() {
    val table =
      P4Table(
        name = "check_ttl",
        keys =
          listOf(
            P4KeyEntry(P4Expr.FieldAccess(P4Expr.Ref("headers"), "ttl"), P4MatchKindRef("exact"))
          ),
        actions = listOf("Send_to_cpu", "NoAction"),
        defaultAction = "NoAction",
        isDefaultActionConst = true,
      )

    assertEquals(
      """
          table check_ttl {
              key = { headers.ttl : exact; }
              actions = {
                  Send_to_cpu;
                  NoAction;
              }
              const default_action = NoAction;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun tableWithStringActions() {
    val dropAction = P4.action("Drop") {}
    val table =
      P4.table("check_ttl") {
        key(P4Expr.Ref("ttl"), P4MatchKindRef("exact"))
        actions(dropAction)
        actionByName("NoAction")
        defaultAction("NoAction", const_ = true)
      }
    assertEquals(
      """
          table check_ttl {
              key = { ttl : exact; }
              actions = {
                  Drop;
                  NoAction;
              }
              const default_action = NoAction;
          }
      """
        .trimIndent(),
      table.toP4(),
    )
  }

  @Test
  fun controlRendering() {
    val ctrl =
      P4Control(
        name = "MyCtrl",
        params = listOf(P4Param("outCtrl", P4Type.Named("OutControl"), Direction.OUT)),
        declarations =
          listOf(
            P4Action(
              "Drop",
              emptyList(),
              listOf(
                P4Statement.Assign(
                  P4Expr.FieldAccess(P4Expr.Ref("outCtrl"), "outputPort"),
                  P4Expr.Ref("DROP_PORT"),
                )
              ),
            )
          ),
        body = listOf(P4Statement.MethodCall(P4Expr.Ref("ipv4_match"), "apply", emptyList())),
      )

    assertEquals(
      """
          control MyCtrl(out OutControl outCtrl) {
              action Drop() {
                  outCtrl.outputPort = DROP_PORT;
              }
              apply {
                  ipv4_match.apply();
              }
          }
      """
        .trimIndent(),
      ctrl.toP4(),
    )
  }
}
