(module
  (import "test" "memory" (memory 1 5))
  (import "test" "function" (func $importedFunction (param i32) (result i32)))

  (func $addOne (param $x i32) (result i32)
    local.get $x
    i32.const 1
    i32.add
  )

  ;; Function 2: Calls $addOne and adds 1 to the result
  (func $addTwo (param $y i32) (result i32)
    local.get $y
    call $addOne
    i32.const 1
    i32.add
  )

  ;; Function 3: Calls $addTwo and adds 1 to the result
  (func $addThree (param $z i32) (result i32)
    local.get $z
    call $addTwo
    i32.const 1
    i32.add
  )
)
