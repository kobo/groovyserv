$(function() {
    // Activate the current page's link in sidebar.
    $(".sidebar a").each(function() {
        if (this.href === document.location.href) {
            $(this).addClass("active");
        }
    });

    // Fix a scroll offset for a link within a page
    $(function () {
        $('a[href^=#]').not('a[href=#]').each(function () {
            if (location.pathname.replace(/^\//, '') === this.pathname.replace(/^\//, '')
                    && location.hostname === this.hostname) {

                $(this).click(function () {
                    var $targetById = $(this.hash);
                    var $targetByAnchor = $('[name=' + this.hash.slice(1) + ']');
                    var target = $targetById.length ? $targetById : ($targetByAnchor.length ? $targetByAnchor : false);
                    if (!target) {
                        return false;
                    }

                    // Set a hash for an address bar (this operation causes scrolling)
                    document.location.hash = this.hash;

                    // Fixing offset
                    // The duration of animate() requires more than 1.
                    // If not, it cannot be work in case of a direct access.
                    var headerOffset = $(".header").height() + 5;
                    $('html, body').animate({ scrollTop: target.offset().top - headerOffset }, 1);

                    // Blinking the target element
                    target
                        .fadeTo('fast', 0.5).fadeTo('slow', 1.0)
                        .fadeTo('fast', 0.5).fadeTo('slow', 1.0)
                        .fadeTo('fast', 0.5).fadeTo('slow', 1.0);
                    return false;
                });
            }
        });

        // For a direct access
        if (location.hash && location.hash !== '#') {
            $('a[href=' + location.hash + ']').click();
        }
    });
});
