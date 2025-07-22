(module
  (import "test" "memory" (memory 1 5))

  (func $if_without_else (param $condition i32) (result i32)
    (local $result i32) ;; Declare a local variable to store the result

    ;; Set a default value for the case where the condition is false.
    (local.set $result (i32.const 0))

    ;; The 'if' block has no 'else'. It only executes if the condition is true.
    (if (local.get $condition)
      (then
        ;; If the condition is true, update the result to 1.
        ;; This block now leaves the stack empty, as expected.
        (local.set $result (i32.const 1))
      )
    )

    ;; Push the final value from the local variable onto the stack to be returned.
    (local.get $result)
  )

  (func $if_with_else (param $condition i32) (result i32)
    (local $result i32) ;; Declare a local variable

    (if (local.get $condition)
      (then
        ;; If true, set the result variable to 1.
        (local.set $result (i32.const 1))
      )
      (else
        ;; If false, set the result variable to 0.
        (local.set $result (i32.const 0))
      )
    )

    ;; Push the final value onto the stack to be returned.
    (local.get $result)
  )
)
