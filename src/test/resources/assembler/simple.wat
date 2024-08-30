(module
    (import "env" "test-global" (global $g0 (mut externref)))

    (func $test (param $r externref) (result externref)
        (local.get $r)
        (global.set $g0)
        (global.get $g0)
    )
    (export "test" (func $test))
)
