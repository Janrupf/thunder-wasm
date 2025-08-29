;; compile-args: --no-check
(module
  (type (;0;) (func (result i32)))
  (func (;0;) (type 0) (result i32)
    block (result i32)  ;; label = @1
      block  ;; label = @2
        unreachable
        i32.const 0
        br_if 1 (;@1;)
      end
      i32.const 0
    end))
