(module
    (func $test (param f64) (result i64)
        (local.get 0)
        (i64.trunc_sat_f64_u)
    )
    (export "test" (func $test))
)
