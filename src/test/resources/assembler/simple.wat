(module
  (import "test" "memory" (memory 1 5))

  (func (export "fill") (param $offset i32) (param $value i32) (param $size i32)
    local.get $offset
    local.get $value
    local.get $size
    memory.fill
  )
)
