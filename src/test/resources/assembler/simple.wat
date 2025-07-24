(module
  (import "module" "mem" (memory $mem 2))
  (import "module" "global" (global $global funcref))
  (import "module" "func" (func $fn (param i32) (result i32)))
  (import "module" "table" (table $tbl 0 funcref))

  (export "mem" (memory $mem))
  (export "global" (global $global))
  (export "func" (func $fn))
  (export "table" (table $tbl))
)
