local Post = {}

function Post.open(main)

    -- Remove everything from the current Lua page
    local children = main:get_children()

    for _, child in ipairs(children) do
        main:remove(child)
    end

    main:show_all()

    -- Run WhenPosted.java
    os.execute("java DragDropVideo.java")
end

return Post