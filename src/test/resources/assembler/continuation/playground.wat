(module
  (import "continuations" "suspender" (func $suspender (param i32) (result i32)))

  (func $other (param $p i32) (result i32)
    (local.get $p)
    (local.get $p)
    (i32.mul)
    (call $suspender)
  )

  (func $doSomething (param $p i32) (result i32)
    (local.get $p)
    (local.get $p)
    (block (param i32) (result i32)
      (call $other)
    )
    (call $other)
    (i32.add)
  )

  (export "doSomething" (func $doSomething))
)