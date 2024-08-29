(module
    (global $g (mut i32) (i32.const 69))

    (func $test (param i32) (result i32)
        (global.get $g)
        (local.get 0)
        (i32.eq)
        (global.set $g)
        (global.get $g)
    )
    (export "test" (func $test))
)
