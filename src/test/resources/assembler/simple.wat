;; Test `unreachable` operator

(module
  (func
    unreachable
    i32.const 0
    i32.const 0
    i32.const 0
    select
    unreachable
    f32.const 0x0p+0 (;=0;)
    i32.const 0
    select
    unreachable)
)
