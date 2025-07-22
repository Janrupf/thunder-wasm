(module
  (import "test" "memory" (memory 1 5))

  (func $if_without_else (param $condition i32) (result i32)
    unreachable
  )
)
