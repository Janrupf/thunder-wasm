;; compile-args: --no-check
(module
  (import "spectest" "memory" (memory 1 2))
  (data (memory 0) (i32.const 10) "\10")

  (func (export "load") (param i32) (result i32) (i32.load (local.get 0)))
)
