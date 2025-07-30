# Thunder WASM

An Experimental Ahead-Of-Time (AOT) WebAssembly Runtime for the JVM

---

![Java](https://img.shields.io/badge/Java-8+-orange)

Thunder WASM is an experimental WebAssembly runtime for the JVM that compiles WebAssembly modules directly to JVM bytecode using an ahead-of-time (AOT) compilation approach. It parses `.wasm` binary files and generates equivalent Java bytecode using the ASM library, allowing WebAssembly modules to run natively on the JVM.

> ### ⚠️ Project Status: Experimental
>
> Thunder WASM is currently in an active research and development phase. It is intended for educational and experimental purposes only and is **not yet suitable for production use**.
>
> The APIs are unstable and may change without notice. While it passes a significant portion of the official WebAssembly test suite, there are known gaps and potential bugs.

## Project Status

### Currently Supported
- ✅ AOT compilation of `.wasm` modules to JVM bytecode
- ✅ Complete WebAssembly MVP instruction set (numeric, memory, control flow, parametric, variable, table, reference)
- ✅ All WebAssembly sections: type, import, function, table, memory, global, export, start, element, code, data
- ✅ Runtime linking of imported/exported functions, globals, tables, and memory
- ✅ Dynamic block splitting for complex control flow
- ✅ Comprehensive test suite against official WebAssembly spec tests

### Not Yet Supported
- ❌ WebAssembly System Interface (WASI)
- ❌ WebAssembly proposals beyond MVP (threads, SIMD, GC, etc.)
- ❌ Advanced debugging and profiling integration

### Overall

Thunder WASM is a work in progress, expect bugs and issues. It does already run a number of WebAssembly modules successfully,
but there are many areas for improvement. There are a large number of instructions that are implemented with a happy weather
approach, meaning they will work correctly when used in the right way, but may not actually trap when they should. This results
in a large number of test suite failures, but also means that many real world modules will run correctly.

## Getting Started

### Prerequisites
- Java 8+ JDK
- Git

### Building from Source

```bash
git clone https://github.com/yourusername/thunder-wasm-v0.git
cd thunder-wasm-v0
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

The test suite includes validation against the official [WebAssembly spec test suite](https://github.com/WebAssembly/spec/tree/main/test) to ensure instruction-level correctness.
Note that a large amount of tests are currently either skipped or fail.

## License

This project is licensed under the LGPL-3.0 License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with the excellent [ASM library](https://asm.ow2.io/) for JVM bytecode generation
- Tested against the official [WebAssembly specification test suite](https://github.com/WebAssembly/spec)
- Inspired by other WebAssembly runtime implementations, mainly be [asmble](https://github.com/cretz/asmble)