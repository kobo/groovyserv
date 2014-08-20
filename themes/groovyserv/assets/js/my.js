$(function() {
    // Activate the current page's link in toc.
    $(".toc a").each(function() {
        if (this.href === document.location.href) {
            $(this).addClass("active");
        }
    });
});
