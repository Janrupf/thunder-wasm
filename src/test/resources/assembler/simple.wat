(module
    (import "env" "test-global" (global $g0 i32))
    (import "env" "test-memory" (memory $m 1))
    (import "env" "test-global-2" (global $g1 i32))

    (global $g2 (mut i32) (i32.const 42))

    (func $test (param i32) (result i32)
        (global.get $g1)
    )
    (export "test" (func $test))
)
