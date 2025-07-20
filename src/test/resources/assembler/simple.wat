(module
  (memory 1)
  (func $load_store_instructions (param $addr i32) (param $value f64)
    ;; Store Instructions
    (local.get $addr)
    (local.get $value)
    (f64.store)
  )
)
