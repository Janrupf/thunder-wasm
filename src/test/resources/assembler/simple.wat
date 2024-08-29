(module
    (func $test (param f64) (param f64) (param i32) (result f64)
        (local.get 0)
        (local.get 1)
        (local.get 2)
        (select (result f64))
    )
    (export "test" (func $test))
)
