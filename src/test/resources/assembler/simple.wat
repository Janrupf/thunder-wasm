(module
  ;; Define 1 page (64KB) of memory
  (import "test" "memory" (memory 1))

  ;; Data segment with test data "WASM" at data segment offset 0
  (data (i32.const 0) "WASM")

  ;; Test function that uses both memory.init and memory.copy
  (func (export "test_memory_ops")
    ;; Test 1: memory.init - copy "WASM" from data segment 0 to memory address 100
    i32.const 100    ;; d: destination offset in memory
    i32.const 0      ;; s: source offset in data segment
    i32.const 4      ;; n: count (4 bytes for "WASM")
    memory.init 0    ;; data segment 0, memory 0

    ;; Test 2: memory.copy - copy the "WASM" from memory address 100 to address 200
    i32.const 50    ;; d: destination offset in memory
    i32.const 100    ;; s: source offset in memory
    i32.const 4      ;; n: count (4 bytes)
    memory.copy      ;; target memory 0, source memory 0
  )

  ;; Helper function to verify data at memory location
  (func (export "read_byte") (param $addr i32) (result i32)
    local.get $addr
    i32.load8_u
  )

  ;; Helper function to read a 32-bit value from memory
  (func (export "read_i32") (param $addr i32) (result i32)
    local.get $addr
    i32.load
  )
)