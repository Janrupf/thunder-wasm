(module
  (import "test" "memory" (memory 1 5))

  (func $test (param $a i32) (param $b i32) (param $c i32) (result i32)
    (local $d i32)
    (local $e i32)

    local.get $b
    local.get $c
    i32.add
    local.set $d

    local.get $e
    local.get $e
    i32.add
    local.set $e

    local.get $a
  )
)
