(module
  (memory 1)
  (func $load_store_instructions (param $addr i32) (param $value i32)
    ;; Store Instructions
    (local.get $addr)
    (local.get $value)
    (i32.store)
  )
)
