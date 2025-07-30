(module
  (func (export "f32.compute_radix") (param $0 f32) (param $1 f32) (result f32)
    (loop $label$0
      (br_if $label$0
        (f32.eq
          (f32.add
            (f32.sub
              (f32.add
                (local.tee $0 (f32.add (local.get $0) (local.get $0)))
                (f32.const 1)
              )
              (local.get $0)
            )
            (f32.const -1)
          )
          (f32.const 0)
        )
      )
    )
    (loop $label$2
      (br_if $label$2
        (f32.ne
          (f32.sub
            (f32.sub
              (f32.add
                (local.get $0)
                (local.tee $1 (f32.add (local.get $1) (f32.const 1)))
              )
              (local.get $0)
            )
            (local.get $1)
          )
          (f32.const 0)
        )
      )
    )
    (local.get $1)
  )

  (func (export "f64.compute_radix") (param $0 f64) (param $1 f64) (result f64)
    (loop $label$0
      (br_if $label$0
        (f64.eq
          (f64.add
            (f64.sub
              (f64.add
                (local.tee $0 (f64.add (local.get $0) (local.get $0)))
                (f64.const 1)
              )
              (local.get $0)
            )
            (f64.const -1)
          )
          (f64.const 0)
        )
      )
    )
    (loop $label$2
      (br_if $label$2
        (f64.ne
          (f64.sub
            (f64.sub
              (f64.add
                (local.get $0)
                (local.tee $1 (f64.add (local.get $1) (f64.const 1)))
              )
              (local.get $0)
            )
            (local.get $1)
          )
          (f64.const 0)
        )
      )
    )
    (local.get $1)
  )
)
