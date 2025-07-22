(module
  (import "test" "memory" (memory 1 5))

  (func $block_example (result i32)
    ;; We will use two local variables. Locals are initialized to 0 by default.
    (local $counter i32) ;; This will be our loop counter, from 10 down to 0.
    (local $accumulator i32) ;; This will store the sum.

    ;; --- Initialization ---
    ;; Set our counter to 10.
    i32.const 10
    local.set $counter

    ;; --- The Loop ---
    ;; A 'loop' instruction creates a block. Branching to its label ($MY_LOOP)
    ;; will jump back to the beginning of this instruction.
    (loop $MY_LOOP

      ;; --- Loop Body: Do the work ---
      ;; Add the current counter value to our accumulator.
      local.get $accumulator  ;; Push current sum onto the stack
      local.get $counter      ;; Push current counter value onto the stack
      i32.add                 ;; Add them together
      local.set $accumulator  ;; Store the new sum

      ;; --- Loop Body: Update the state ---
      ;; Decrement the counter.
      local.get $counter
      i32.const 1
      i32.sub                 ;; Subtract 1 from the counter
      local.set $counter

      ;; --- Loop Condition: Decide whether to loop again ---
      ;; Get the new value of the counter.
      local.get $counter
      ;; If the counter is not zero, the 'br_if' will trigger.
      ;; 'br_if $MY_LOOP' means "branch to the label $MY_LOOP if the
      ;; value on top of the stack is not 0 (true)".
      br_if $MY_LOOP
    ) ;; The loop ends here when the counter becomes 0 and br_if does not fire.

    ;; --- Return the result ---
    ;; The final sum is in our accumulator. We push it to the stack
    ;; to be returned by the function.
    local.get $accumulator
  )
)
