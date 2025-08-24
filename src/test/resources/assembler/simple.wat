(module
  (func (export "f32.no_fold_div_neg0") (param $x f32) (result f32)
    (f32.div (local.get $x) (f32.const -0.0)))
  (func (export "f64.no_fold_div_neg0") (param $x f64) (result f64)
    (f64.div (local.get $x) (f64.const -0.0)))
)