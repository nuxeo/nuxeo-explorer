(function ($) {

  $.fn.toc = function (tocList) {
    // use find() so that the argument is always interpreted as a selector, never as HTML
    const $tocList = $(document).find(tocList);
    $tocList.empty();
    $(".toc").each(function (index) {
      const name = `toc${index}`;
      $(this).before($("<a></a>").attr({ name: name, "class": "anchor" }));
      $tocList.append($("<li></li>").append($("<a></a>").attr("href", `#${name}`).text($(this).text())));
    });
  }

})(jQuery);
