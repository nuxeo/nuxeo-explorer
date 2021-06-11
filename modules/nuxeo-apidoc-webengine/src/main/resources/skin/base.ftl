<!DOCTYPE html>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>
    <@block name="title">Nuxeo Platform Explorer</@block>
  </title>
  <meta http-equiv="Content-Type" charset="UTF-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <meta name="author" content="Nuxeo">
  <meta name="description" content="Nuxeo Platform Explorer">

  <link rel="shortcut icon" href="${skinPath}/images/favicon.png" />
  <link rel="stylesheet" href="${skinPath}/css/apidoc_style.css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/css/code.css" media="screen" charset="utf-8" />
  <link rel="stylesheet" href="${skinPath}/css/jquery.magnific.min.css" media="screen" charset="utf-8">
  <@block name="stylesheets" />

  <script src="${skinPath}/script/jquery/jquery-1.7.2.min.js"></script>
  <script src="${skinPath}/script/jquery/cookie.js"></script>
  <script src="${skinPath}/script/highlight.js"></script>
  <script src="${skinPath}/script/java.js"></script>
  <script src="${skinPath}/script/html-xml.js"></script>
  <script src="${skinPath}/script/manifest.js"></script>
  <script src="${skinPath}/script/jquery.magnific.min.js"></script>
  <script src="${skinPath}/script/jquery.highlight-3.js"></script>
  <script src="${skinPath}/script/jquery.toc.js"></script>
  <script src="${skinPath}/script/jquery.utils.js"></script>
  <script src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
  <script src="${skinPath}/script/plotly-1.58.4.min.js"></script>
  <script src="${skinPath}/script/plotly.chart.js"></script>
  <@block name="header_scripts" />

</head>

<body>

<#if !Root.isEmbeddedMode()>
  <header role="banner">
    <@block name="header">
    <div class="top-banner">
      <a href="${Root.path}" class="logo">
        <svg version="1.0" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 515 105">
          <switch>
            <g>
              <path fill="#FFF" d="M250.64 8.88h.5v86.76h-.5z" />
              <g fill="#FFF">
                <path d="M45.01 72.94l4.92-.87V33.59l-4.8-.52v-2.85h15.22v2.85l-4.75.49v18.16h22.33V33.56l-4.8-.49v-2.85h15.14v2.85l-4.69.49v38.51l5.01.87v2.47H72.93v-2.47l5.01-.84V55.16H55.61v16.91l5.01.87v2.47h-15.6v-2.47z" />
                <path d="M90.88 85.28c.33.1.71.17 1.15.23.44.06.83.09 1.18.09.7 0 1.46-.13 2.27-.38.82-.25 1.64-.74 2.47-1.47.83-.73 1.64-1.76 2.43-3.1.79-1.34 1.5-3.09 2.14-5.24h-1.25L89.6 46.45l-3.03-.52v-3.09h13.48v3.09l-4.19.58 6.84 19.53 1.95 5.94 1.69-5.94 5.91-19.62-4.54-.5v-3.09h12.57v3.09l-3.26.47c-1.3 3.77-2.44 7.07-3.42 9.93-.98 2.85-1.83 5.33-2.56 7.42-.73 2.1-1.35 3.87-1.86 5.33s-.95 2.67-1.3 3.64c-.35.97-.63 1.75-.84 2.34-.21.59-.4 1.08-.55 1.47-.76 1.94-1.52 3.69-2.29 5.24-.77 1.55-1.58 2.9-2.43 4.03-.85 1.14-1.77 2.07-2.75 2.79-.98.73-2.07 1.24-3.27 1.53-.78.19-1.63.29-2.56.29a7.544 7.544 0 01-1.32-.12c-.22-.04-.42-.09-.6-.15-.17-.06-.3-.12-.38-.17v-4.68z" />
                <path d="M118.67 72.94l4.95-.61V32.92l-4.66-1.05v-2.53l9.11-1.22 1.16.67v43.55l5.18.61v2.47h-15.75v-2.48zM134.57 66.04c.14-1.36.57-2.55 1.31-3.57.74-1.02 1.65-1.9 2.74-2.63 1.09-.74 2.3-1.35 3.63-1.83s2.66-.87 4-1.16c1.34-.29 2.63-.49 3.87-.61 1.24-.12 2.32-.17 3.23-.17v-2.53c0-1.53-.2-2.78-.61-3.74-.41-.96-.97-1.71-1.69-2.26-.72-.54-1.56-.91-2.53-1.11-.97-.19-2.02-.29-3.14-.29-.56 0-1.19.05-1.89.16s-1.42.26-2.17.45-1.5.43-2.26.7c-.76.27-1.48.57-2.15.9l-1.08-2.62c.72-.58 1.6-1.09 2.63-1.53 1.04-.44 2.11-.8 3.2-1.09 1.1-.29 2.16-.5 3.2-.64s1.92-.2 2.66-.2c1.53 0 2.99.16 4.38.48 1.39.32 2.6.88 3.65 1.69 1.05.81 1.88 1.89 2.49 3.26.61 1.37.92 3.11.92 5.23l-.06 20.03h3.73v1.92c-.68.37-1.52.67-2.52.9-1 .23-2 .35-3.01.35-.35 0-.73-.02-1.14-.07a3.01 3.01 0 01-1.12-.36c-.34-.19-.62-.5-.84-.9-.22-.41-.34-.97-.34-1.69v-.82c-.45.54-.98 1.05-1.59 1.51-.61.47-1.31.87-2.1 1.22s-1.68.62-2.68.82c-1 .19-2.11.29-3.33.29-1.51 0-2.89-.27-4.12-.81s-2.26-1.28-3.09-2.2a8.711 8.711 0 01-1.83-3.2c-.38-1.23-.5-2.52-.35-3.88zm5.65.12c0 1.15.15 2.12.47 2.91.31.8.76 1.44 1.35 1.94.59.5 1.31.85 2.17 1.08.85.22 1.81.33 2.88.33.58 0 1.17-.08 1.77-.23.6-.16 1.18-.35 1.73-.58.55-.23 1.07-.5 1.54-.8.48-.3.86-.6 1.15-.89 0-1.82.01-3.63.02-5.43.01-1.79.02-3.6.04-5.43-.81 0-1.71.04-2.68.13-.97.09-1.94.24-2.91.45-.97.21-1.91.5-2.82.84-.91.35-1.72.79-2.42 1.32s-1.26 1.16-1.67 1.88c-.41.72-.62 1.55-.62 2.48zM163.65 72.94l3.96-.58V48.64l-4.13-2.27v-1.8l7.74-2.3 1.31.58v4.16c.66-.56 1.45-1.12 2.39-1.67.93-.55 1.92-1.05 2.97-1.5 1.05-.45 2.12-.81 3.23-1.09 1.11-.28 2.15-.44 3.14-.48 1.81-.06 3.32.16 4.54.66 1.22.5 2.21 1.3 2.97 2.42.76 1.12 1.3 2.55 1.63 4.29.33 1.75.5 3.84.5 6.29v16.45l4.69.58v2.47h-14.67v-2.47l4.4-.61v-15.4c0-1.77-.09-3.32-.26-4.66-.17-1.34-.52-2.45-1.03-3.32a4.635 4.635 0 00-2.14-1.92c-.91-.41-2.11-.58-3.58-.52-1.2.06-2.47.33-3.81.8-1.34.48-2.75 1.22-4.22 2.23v22.79l4.34.61v2.47h-13.94v-2.49zM209.94 76.11c-1.63 0-3.21-.32-4.74-.97-1.53-.65-2.89-1.65-4.06-3-1.17-1.35-2.12-3.06-2.84-5.12-.72-2.07-1.08-4.52-1.08-7.35 0-2.6.44-4.99 1.31-7.17.87-2.18 2.07-4.06 3.58-5.63 1.51-1.57 3.29-2.8 5.34-3.68 2.05-.88 4.24-1.32 6.59-1.32 1.03 0 2.03.07 3 .2.97.14 1.81.32 2.5.55v-9.69l-6.2-1.05v-2.53l10.48-1.22 1.31.67v44.16h3.93v2.18c-.33.17-.74.34-1.24.49-.5.16-1.02.29-1.57.41s-1.12.2-1.69.26c-.57.06-1.11.09-1.61.09-.35 0-.71-.02-1.09-.07-.38-.05-.72-.17-1.03-.36-.31-.19-.57-.5-.77-.9-.2-.41-.31-.97-.31-1.69v-.73c-.29.33-.72.7-1.28 1.11-.56.41-1.26.79-2.1 1.14-.84.35-1.79.65-2.87.89-1.07.21-2.26.33-3.56.33zm2.24-3.98c.62 0 1.2-.03 1.73-.1s1.03-.16 1.5-.28c.95-.23 1.77-.54 2.45-.93a8.33 8.33 0 001.69-1.25V47.3c-.21-.25-.48-.51-.79-.79-.31-.27-.71-.52-1.21-.76-.5-.23-1.09-.42-1.78-.57-.69-.15-1.5-.22-2.43-.22-.74 0-1.5.09-2.3.28-.8.18-1.58.49-2.36.92-.78.43-1.51.98-2.21 1.66-.7.68-1.32 1.51-1.86 2.49-.54.98-.98 2.13-1.31 3.43-.33 1.31-.51 2.81-.52 4.5-.04 2.39.2 4.45.73 6.2.52 1.75 1.23 3.19 2.11 4.32.88 1.14 1.9 1.98 3.04 2.53 1.14.56 2.31.84 3.52.84z" />
              </g>
              <path fill="#FFF" d="M226.75 28.18h3.02v.75h-1v3.74h-.83v-3.74h-1.19v-.75zm3.48 0h1.09l1 3.14 1.07-3.14h1.06v4.51h-.78v-3.38l-1 3.15h-.65l-1-3.15v3.38h-.78v-4.51z" />
              <path fill="#73D2CF" d="M348.28 42.11v6.78l9.96 9.9-9.96 9.9v6.78h6.82l9.95-9.9 9.96 9.9h6.81v-6.78l-9.95-9.9 9.95-9.9v-6.78h-6.81l-9.96 9.91-9.95-9.91z" />
              <path fill="#FFF" d="M337.1 42.11v25.37h-16.77V42.11h-8.39v25.02l8.39 8.34h25.16V42.11zM300.76 42.11H275.6v33.36h8.39V50.11h16.77v25.36h8.38V50.45zM446.11 67.48h-16.77V50.11h16.77v17.37zm0-25.37h-25.19v25.05l8.39 8.34h25.19V50.45l-8.39-8.34zM418.16 50.11v-8h-33.54v33.36h33.54v-7.99H393v-4.81h24.81v-7.93H393v-4.63z" />
            </g>
          </switch>
        </svg>
      </a>
      <a href="${Root.path}" class="home">Platform Explorer</a>
      <#if Root.currentDistribution != null>
        <span>
          / <a href="${Root.path}/${Root.currentDistribution.key}/">${Root.currentDistribution.name} ${Root.currentDistribution.version}</a>
        </span>
      </#if>
      <div class="login">
        <#include "nxlogin.ftl">
      </div>
    </div>
    </@block>

    <nav role="navigation">
      <@block name="left">
        <#include "nav.ftl">
      </@block>
    </nav>
  </header>
</#if>

<div class="container content">
  <@block name="middle">
    <section>
      <article role="contentinfo">
        <#if onArtifact?? && !Root.isRunningFunctionalTests()>
          <@block name="googleSearchFrame">
            <@googleSearchFrame This.searchCriterion />
          </@block>
        </#if>
        <#if successFeedbackMessage??>
          <div id="successMessage" class="message success">
            ${successFeedbackMessage?html}
          </div>
        </#if>
        <#if errorFeedbackMessage??>
          <div id="errorMessage" class="message error">
            ${errorFeedbackMessage?html}
          </div>
        </#if>
        <@block name="right">
          Content
        </@block>
      </article>
    </section>
  </@block>
</div>
<script type="text/javascript">
  hljs.initHighlightingOnLoad();
</script>

<@block name="footer_scripts" />
<@ga />
</body>
</html>
<#macro ga>
<script>
  !function() {
    if (window.location.host.match(/localhost/)) {
      // Skip analytics tracking on localhost
      return;
    }

    var analytics = window.analytics = window.analytics || [];
    if (!analytics.initialize) {
      if (analytics.invoked) {
        window.console && console.error && console.error("Segment snippet included twice.");
      } else {
        analytics.invoked = !0;
        analytics.methods = ["trackSubmit", "trackClick", "trackLink", "trackForm", "pageview", "identify", "reset", "group", "track", "ready", "alias", "page", "once", "off", "on"];
        analytics.factory = function(t) {
          return function() {
            var e = Array.prototype.slice.call(arguments);
            e.unshift(t);
            analytics.push(e);
            return analytics
          }
        };
        for (var t = 0; t < analytics.methods.length; t++) {
          var e = analytics.methods[t];
          analytics[e] = analytics.factory(e)
        }
        analytics.load = function(t) {
          var e = document.createElement("script");
          e.type = "text/javascript";
          e.async = !0;
          e.src = ("https:" === document.location.protocol ? "https://" : "http://") + "cdn.segment.com/analytics.js/v1/" + t + "/analytics.min.js";
          var n = document.getElementsByTagName("script")[0];
          n.parentNode.insertBefore(e, n)
        };
        analytics.SNIPPET_VERSION = "3.1.0";
        analytics.load("4qquvje3fv");
        analytics.page()
      }
    }
  }();
</script>
</#macro>
