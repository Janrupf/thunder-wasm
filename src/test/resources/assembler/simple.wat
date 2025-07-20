(module
  (import "module" "table" (table $imported_table 10 20 funcref))

  (func (param $index i32) (param $func_ref funcref)
    ref.null func
    i32.const 5
    table.grow $imported_table
    drop
    local.get $index
    local.get $func_ref
    table.set $imported_table
  )
)
