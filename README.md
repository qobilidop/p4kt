# P4kt

[![CI](https://github.com/qobilidop/p4kt/actions/workflows/ci.yml/badge.svg)](https://github.com/qobilidop/p4kt/actions/workflows/ci.yml)
[![Docs](https://github.com/qobilidop/p4kt/actions/workflows/docs.yml/badge.svg)](https://qobilidop.github.io/p4kt/)

P4kt (pronounced "packet") is an embedded domain-specific language (eDSL) for [P4](https://p4.org) in Kotlin.

## Why eDSL for P4?

P4 is great at describing packet processing, but it has no way to programmatically generate or compose designs beyond what the language's type system allows. P4 does support the C preprocessor, but preprocessor macros are fragile, untyped, and error-prone.

An eDSL gives you the full power of a general-purpose language while staying in the P4 domain. You write P4 concepts - headers, parsers, tables - but with the host language's abstraction, composition, and tooling.

## How it works

P4kt is a **code generation eDSL**. You construct a P4 program using Kotlin's nested builder syntax, then render it to P4 source text. The output feeds into the existing P4 toolchain - compilers, simulators, and hardware targets all work unchanged. Generated P4 can be mixed with hand-written P4 or output from other tools, and the output is always inspectable.

## Why Kotlin?

Kotlin has [purpose-built support for eDSLs](https://kotlinlang.org/docs/type-safe-builders.html) - [lambda receivers](https://kotlinlang.org/docs/lambdas.html#function-literals-with-receiver) for nested builder syntax, [operator overloading](https://kotlinlang.org/docs/operator-overloading.html) for domain expressions, and [`@DslMarker`](https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker) for scope control. These are exactly the features that P4's hierarchical structure (packages, controls, tables, actions) calls for. This makes P4kt a **builder eDSL** - one that constructs programs through nested, type-safe builder calls - rather than relying on advanced type system features like monads or type-level programming. That narrows the field considerably.

Here's how the alternatives compare:

- **Haskell** - the gold standard for deeply embedded eDSLs, with type classes, monads, and higher-kinded types. [Clash](https://clash-lang.org/) uses Haskell's functional abstractions to describe hardware and compiles to VHDL/Verilog. But these features are more than a P4 code generation eDSL needs, and the learning curve is steep for a project where simpler tools suffice.
- **Scala** - the strongest JVM language for eDSLs, with implicit conversions, macros, and higher-kinded types. [Chisel](https://www.chisel-lang.org/) and [SpinalHDL](https://github.com/SpinalHDL/SpinalHDL), both hardware description eDSLs, demonstrate Scala's strength in a neighboring domain. But Scala has a steeper learning curve than Kotlin.
- **OCaml** - excellent for compiler and eDSL work, with a powerful module system and algebraic data types. OCaml has real presence in the P4 ecosystem - [Petr4](https://github.com/verified-network-toolchain/petr4) provides formal semantics and a definitional interpreter for P4, and [Hardcaml](https://github.com/janestreet/hardcaml) is a hardware eDSL. But OCaml's strength is in analysis and verification rather than ergonomic builder eDSLs.
- **Rust** - powerful type system, but its eDSL approach relies on procedural macros operating on token streams - closer to code generation than true embedding. Builder eDSLs are verbose compared to Kotlin's lambda receivers.
- **C++** - the dominant language in networking and the language of the P4 reference compiler [P4C](https://github.com/p4lang/p4c). Template metaprogramming enables powerful eDSLs like [Boost.Spirit](https://www.boost.org/doc/libs/release/libs/spirit/), but they are hard to write, debug, and read.
- **Python** - a strong alternative with different tradeoffs. Python eDSLs like [Amaranth HDL](https://amaranth-lang.org/) perform rich domain-specific checking at elaboration time, catching width mismatches and invalid connections with clear messages. Python offers a lower barrier to entry and a massive ecosystem. The tradeoff is that structural errors - typos in names, wrong types passed to builders, accidentally nesting tables under actions - surface when you run the script rather than in the IDE as you type.

Kotlin is more accessible than Haskell or Scala, better suited for builder eDSLs than OCaml, more expressive for eDSLs than C++ or Rust, and catches structural errors at compile time with IDE feedback where Python cannot - though domain-specific checks like bit-width validation happen at code generation time in both approaches. Kotlin is also the language behind [Gradle's Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html) and [Jetpack Compose](https://developer.android.com/compose), two of the most widely-used eDSLs in industry.
