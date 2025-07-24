(module
  ;; Define a type for a function that takes an i32 and an f64, and returns an f64.
  (type $i32_f64_to_f64 (func (param i32) (param f64) (result f64)))

  (import "test" "hello" (func $test_hello (type $i32_f64_to_f64)))

  ;; Define a table that can hold function references. We'll initialize it with one element.
  (table $func_table 1 funcref)

  ;; Initialize the table at index 0 with the function reference for $perform_op.
  (elem (i32.const 0) $test_hello)

  ;; Define a function that matches the signature $i32_f64_to_f64.
  ;; This function will convert the integer to a float, add it to the float parameter,
  ;; and return the result.
  (func $perform_op (type $i32_f64_to_f64)
    (local.get 1)                 ;; Get the f64 parameter
    (local.get 0)                 ;; Get the i32 parameter
    (f64.convert_i32_s)           ;; Convert the i32 to an f64
    (f64.add)                     ;; Add the two f64 values
  )

  ;; Export a "dispatch" function to be called from the host environment (e.g., JavaScript).
  ;; It takes an index, an i32, and an f64.
  (func (export "dispatch") (param $index i32) (param $val1 i32) (param $val2 f64) (result f64)
    (local.get 1) ;; Push the i32 parameter for the indirect call
    (local.get 2) ;; Push the f64 parameter for the indirect call
    (local.get 0) ;; Push the table index for the indirect call
    (call_indirect (type $i32_f64_to_f64)) ;; Perform the indirect call with the specified type
  )

  (func (export "direct_call") (result f64)
    (i32.const 2)
    (f64.const 42)
    (call $test_hello)
  )
)
