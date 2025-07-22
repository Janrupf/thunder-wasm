(module
  (import "test" "memory" (memory 1 5))

  (func $block_example (result i32)
    (block (result i32 i32)
      i32.const 1
      i32.const 2
    )

    (loop (param i32) (result i32 i32)
      drop
      i32.const 4
      i32.const 5
      br 0
    )

    drop
    drop
  )
)
