(module
  (import "test" "memory" (memory 1 5))

  (func $block_example (param $index i32) (result i32)
    block $B3
      block $B2
        block $B1
          block $B0
            local.get $index
            br_table $B0 $B1 $B2 $B3
          end
          i32.const 101
          return
        end
        i32.const 102
        return
      end
      i32.const 103
      return
    end
    i32.const 99 ;; Default value
  )
)
