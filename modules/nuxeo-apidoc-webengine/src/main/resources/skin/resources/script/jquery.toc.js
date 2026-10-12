(function ($) {

  $.fn.toc = function (tocList) {
    const $tocList = $(document).find(tocList);
    $tocList.empty();
    $(".toc").each(function (index) {
      const name = `toc${index}`;
      const text = $(this).text();
      $(this).before($("<a>").attr({ name: name, class: "anchor" }));
      $tocList.append($("<li>").append($("<a>").attr("href", `#${name}`).text(text)));
    });
  }

})(jQuery);
