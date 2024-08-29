(module
    (func $exported (export "function") (param i32) (result i32)
        i32.const 42
    )
    (memory (export "memory") 1 12)
    (table (export "table") 1 12 funcref)
    (global (export "global") (mut i32) (i32.const 42))
)
