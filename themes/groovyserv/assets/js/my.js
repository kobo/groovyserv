$(function() {
    // Activate the current page's link in sidebar.
    $(".sidebar a").each(function() {
        if (this.href === document.location.href) {
            $(this).addClass("active");
        }
    });
});
