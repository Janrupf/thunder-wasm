;; compile-args: --no-check
(module
  (global $g (import "test" "g") (mut i32))
  (memory 1)
  (data (global.get $g))
)
