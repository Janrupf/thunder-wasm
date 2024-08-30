(module
    (import "env" "test-global" (global $g0 (mut i32)))

    (global $g1 (mut i32) (i32.const 42))

    (func $test (param i32) (result i32)
        (global.get $g0)
        (global.set $g1)
        (global.get $g1)
    )
    (export "test" (func $test))
)
