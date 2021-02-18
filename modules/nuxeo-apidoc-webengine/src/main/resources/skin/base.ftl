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
  <@block name="header_scripts" />

</head>

<body>

<#if !Root.isEmbeddedMode()>
  <header role="banner">
    <@block name="header">
    <div class="top-banner">
      <a href="${Root.path}">
        <span class="main-menu--logo svg">
          <svg viewBox="0 0 128 24" xmlns="http://www.w3.org/2000/svg">
            <path fill="#fff" d="M0,0 18,0 24,6 24,24 18,24 18,6 6,6 6,24 0,24M26,0 32,0 32,18 44,18 44,0 50,0 50,24 32,24 26,18M78,0 102,0 102,6 84,6 84,9 102,9 102,15 84,15 84,18 102,18 102,24 78,24M104,0 122,0 128,6 128,24 110,24 104,18 104,0 110,6 110,18 122,18 122,6 110,6z" />
            <path fill="#73d2cf" d="M52,0 57,0 76,19 76,24 71,24 52,5M52,24 52,19 71,0 76,0 76,5 57,24z" />
          </svg>
        </span>
      </a>
      <a href="${Root.path}">
        <span>Platform Explorer</span>
      </a>
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
