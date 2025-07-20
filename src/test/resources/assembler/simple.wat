(module
  (memory 1)
  (func $load_store_instructions (param $addr i32) (result i64)
    ;; Store Instructions
    (local.get $addr)
    (i64.load32_s offset=4)
  )
)
