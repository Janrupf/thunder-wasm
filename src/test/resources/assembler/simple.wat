(module
    (elem $elem func 0 1 2 3)

    (table $func-table 10 funcref)

    (func $test (param externref) (result i32)
        (i32.const 3)
        (i32.const 1)
        (i32.const 2)
        (table.init $func-table $elem)
        (i32.const 0)
    )
    (func)
    (func)
    (func)
    (export "test" (func $test))
)
