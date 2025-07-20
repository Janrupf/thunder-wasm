(module
  ;; -- 1. Function and Table Definitions --
  (type $return_i32 (func (result i32)))

  ;; Functions to serve as our reference targets.
  (func $f1 (type $return_i32) (result i32) i32.const 11)
  (func $f2 (type $return_i32) (result i32) i32.const 22)
  (func $f3 (type $return_i32) (result i32) i32.const 33)
  (func $f4 (type $return_i32) (result i32) i32.const 44)

  ;; Active and Passive tables.
  (table $t_active 2 funcref)
  (table $t_passive 2 funcref)

  ;; -- 2. Element Segments (Corrected Syntax) --
  ;; ACTIVE segment targeting `$t_active`.
  (elem (table $t_active) (offset (i32.const 0)) func $f1 $f2)

  ;; PASSIVE segment (with index 0). The `passive` keyword is optional.
  (elem func $f3 $f4)

  ;; -- 3. Initialization and Verification Functions (Exported) --

  ;; Corrected `init_passive_table`.
  ;; It pushes the arguments to the stack, then calls the instruction.
  (func (export "init_passive_table")
    (i32.const 0) ;; Destination offset in table `$t_passive`
    (i32.const 0) ;; Source offset in element segment 0
    (i32.const 2) ;; Number of elements to copy
    ;; The instruction itself takes immediate arguments for the table and segment indices.
    (table.init $t_passive 0) ;; Initialize table `$t_passive` from elem segment 0

    ;; Drop the element segment to free resources.
    (elem.drop 0)
  )

  ;; Verifies if slots in the ACTIVE table are initialized (not null).
  ;; Returns 1 if initialized, 0 if not.
  (func (export "is_active_initialized") (param $idx i32) (result i32)
    (local.get $idx)      ;; Get the index for the table
    (table.get $t_active) ;; Get the reference from the table slot
    (ref.is_null)         ;; Pushes 1 if it's null, 0 if it's not
    (i32.eqz)             ;; Flips the result: pushes 1 if input was 0, and 0 if input was 1
  )

  ;; Verifies if slots in the PASSIVE table are initialized (not null).
  ;; Returns 1 if initialized, 0 if not.
  (func (export "is_passive_initialized") (param $idx i32) (result i32)
    (local.get $idx)
    (table.get $t_passive)
    (ref.is_null)
    (i32.eqz)
  )
)