(module
    (func $testFunction (param i32) (result i32)
        i32.const 1
    )

    (func $testFunction2 (param i32) (result i32)
        i32.const 2
    )

    (func (param i64) (param i32) (param i64) (param f64) (param externref) (param funcref) (result i32) (result i32)
        (i32.const 42)
        (i32.const 64)
    )
)
