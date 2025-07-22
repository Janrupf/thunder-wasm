(module
  (import "test" "memory" (memory 1 5))

  (func $block_example (result i32)
    (block (result i32 f32)
      i32.const 5
      f32.const 1.0
      i32.const 10
      return
    )

    drop
  )
)
