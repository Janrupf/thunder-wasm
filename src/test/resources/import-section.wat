(module
    (import "someModule" "someFunction" (func $someFunction (param i32) (result i32)))
    (import "someModule" "memory" (memory 1 12))
    (import "someModule" "table" (table 1 12 funcref))
    (import "someModule" "global" (global $someGlobal (mut i32)))
)
