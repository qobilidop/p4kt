# Examples

Each example is a standalone P4kt program alongside the P4 code it produces.
Source files are in the [`examples/`](https://github.com/qobilidop/p4kt/tree/main/examples) directory.

## VSS architecture

The switch architecture declarations from [`very_simple_model.p4`](https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/very_simple_model.p4) - typedefs, constants, and struct definitions.

## VSS example

The user program from [`vss-example.p4`](https://github.com/p4lang/p4c/blob/main/testdata/p4_16_samples/vss-example.p4) - headers, structs, and actions for Ethernet/IPv4 forwarding.

## v1model basic

The supported subset of [`basic.p4`](https://github.com/p4lang/tutorials/blob/master/exercises/basic/solution/basic.p4) from the p4lang tutorials - IPv4 forwarding on the v1model architecture. Missing features: default select case, `update_checksum` (needs list expressions and `HashAlgorithm` enum).

## Future examples

The following [p4lang/tutorials](https://github.com/p4lang/tutorials/tree/master) programs are good candidates for future examples. Each demonstrates P4 language features not already covered by basic.p4. They are listed roughly in order of how close the DSL is to supporting them.

### advanced_tunnel

[`advanced_tunnel.p4`](https://github.com/p4lang/tutorials/blob/master/exercises/p4runtime/advanced_tunnel.p4) - tunnel encapsulation/decapsulation with packet counters.

Needs: counter extern (`CounterType` enum), `&&`/`!` operators, cast expressions.

### source_routing

[`source_routing.p4`](https://github.com/p4lang/tutorials/blob/master/exercises/source_routing/solution/source_routing.p4) - source-routed forwarding with a variable-length header stack.

Needs: header stacks, parser loops (`.next`, `.last`), `pop_front`, array indexing (`[0]`), cast expressions.

### firewall

[`firewall.p4`](https://github.com/p4lang/tutorials/blob/master/exercises/firewall/solution/firewall.p4) - stateful firewall using bloom filters to track TCP connections.

Needs: register extern (extern-level type params), `hash()` extern function, `.apply().hit`.

### calc

[`calc.p4`](https://github.com/p4lang/tutorials/blob/master/exercises/calc/solution/calc.p4) - a calculator protocol with arithmetic and bitwise operations.

Needs: multi-field select (tuples), `+`/`&`/`|`/`^` operators, `lookahead` with type params, `const entries` in tables.
