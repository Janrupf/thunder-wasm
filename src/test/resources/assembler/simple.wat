(module
    (func $test (param f32) (result i32)
        (local.get 0)
        (i32.trunc_f32_u)
    )
    (export "test" (func $test))
)
