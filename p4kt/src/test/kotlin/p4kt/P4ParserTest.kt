package p4kt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class P4ParserTest {
  @Test
  fun parserStateRendering() {
    val state = P4ParserState("start", listOf(P4Statement.Transition("accept")))
    assertEquals(
      """
                state start {
                    transition accept;
                }
            """
        .trimIndent(),
      state.toP4(),
    )
  }

  @Test
  fun parserRendering() {
    val parser =
      P4Parser(
        name = "TopParser",
        params =
          listOf(
            P4Param("b", P4Type.Named("packet_in"), null),
            P4Param("p", P4Type.Named("Parsed_packet"), Direction.OUT),
          ),
        declarations = emptyList(),
        states = listOf(P4ParserState("start", listOf(P4Statement.Transition("accept")))),
      )
    assertEquals(
      """
                parser TopParser(packet_in b, out Parsed_packet p) {
                    state start {
                        transition accept;
                    }
                }
            """
        .trimIndent(),
      parser.toP4(),
    )
  }

  @Test
  fun parserWithDeclarationsAndStates() {
    val parser =
      P4Parser(
        name = "TopParser",
        params =
          listOf(
            P4Param("b", P4Type.Named("packet_in"), null),
            P4Param("p", P4Type.Named("Parsed_packet"), Direction.OUT),
          ),
        declarations = listOf(P4ExternInstance("Ck16", "ck")),
        states =
          listOf(
            P4ParserState(
              "start",
              listOf(
                P4Statement.MethodCall(
                  P4Expr.Ref("b"),
                  "extract",
                  listOf(P4Expr.FieldAccess(P4Expr.Ref("p"), "ethernet")),
                ),
                P4Statement.TransitionSelect(
                  P4Expr.FieldAccess(P4Expr.FieldAccess(P4Expr.Ref("p"), "ethernet"), "etherType"),
                  listOf(P4Expr.Lit(0x0800) to "parse_ipv4"),
                ),
              ),
            ),
            P4ParserState(
              "parse_ipv4",
              listOf(
                P4Statement.MethodCall(
                  P4Expr.Ref("b"),
                  "extract",
                  listOf(P4Expr.FieldAccess(P4Expr.Ref("p"), "ip")),
                ),
                P4Statement.Transition("accept"),
              ),
            ),
          ),
      )
    assertEquals(
      """
                parser TopParser(packet_in b, out Parsed_packet p) {
                    Ck16() ck;
                    state start {
                        b.extract(p.ethernet);
                        transition select(p.ethernet.etherType) {
                            2048 : parse_ipv4;
                        }
                    }
                    state parse_ipv4 {
                        b.extract(p.ip);
                        transition accept;
                    }
                }
            """
        .trimIndent(),
      parser.toP4(),
    )
  }

  @Test
  fun paramAcceptsTypeReference() {
    val externType = P4Extern("packet_in", emptyList())

    val program =
      P4.program {
        @Suppress("UnusedPrivateProperty")
        val TopParser by parser {
          val b by param(externType)

          @Suppress("UnusedPrivateProperty") val start by state { transition(P4.accept) }
        }
      }

    assertTrue(program.toP4().contains("packet_in b"))
  }

  @Test
  fun parserReordersStartStateFirst() {
    val parser =
      P4Parser(
        name = "MyParser",
        params = emptyList(),
        declarations = emptyList(),
        states =
          listOf(
            P4ParserState("parse_ipv4", listOf(P4Statement.Transition("accept"))),
            P4ParserState("start", listOf(P4Statement.Transition("parse_ipv4"))),
          ),
      )
    val output = parser.toP4()
    val startIndex = output.indexOf("state start")
    val parseIndex = output.indexOf("state parse_ipv4")
    assertTrue(startIndex < parseIndex, "start state should appear before parse_ipv4")
  }

  @Test
  fun parserDsl() {
    class Ethernet_h(base: P4Expr) : P4.HeaderRef(base) {
      val etherType by field(P4Type.Bit(16))
    }

    class Parsed_packet(base: P4Expr) : P4.StructRef(base) {
      val ethernet by field(::Ethernet_h)
      val ip by field(P4Type.Named("Ipv4_h"))
    }

    val program =
      P4.program {
        struct(::Parsed_packet)

        @Suppress("UnusedPrivateProperty")
        val TopParser by parser {
          val b by param(P4.typeName("packet_in"))
          val p by param(::Parsed_packet, P4.OUT)

          val parse_ipv4 by state {
            call(b, "extract", p.ip)
            transition(P4.accept)
          }

          @Suppress("UnusedPrivateProperty")
          val start by state {
            call(b, "extract", p.ethernet.expr)
            select(p.ethernet.etherType) { P4.lit(0x0800) to parse_ipv4 }
          }
        }
      }

    val output = program.toP4()
    assertTrue(output.contains("state start"), "should contain start state")
    assertTrue(output.contains("state parse_ipv4"), "should contain parse_ipv4 state")
    val startIndex = output.indexOf("state start")
    val parseIndex = output.indexOf("state parse_ipv4")
    assertTrue(startIndex < parseIndex, "start should come before parse_ipv4")
  }
}
