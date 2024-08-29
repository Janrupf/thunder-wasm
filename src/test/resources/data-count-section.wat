(module
    (memory 1 1)
    (data (i32.const 0) "Hello, World!")

    (func
        ;; Force the data-count section to be included
        (data.drop 0)
    )
)
