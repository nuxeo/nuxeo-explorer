<@extends src="base.ftl">
<@block name="title">Extension point ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Extension point <span class="componentTitle">${nxItem.name}</span></h1>
<div class="include-in components">In component <a href="${Root.path}/${distId}/viewComponent/${nxItem.componentId}">${nxItem.componentId}</a></div>

<div class="tabscontent">
  <@toc />

  <#if nxItem.documentationHtml?has_content>
    <h2 class="toc">Documentation</h2>
    <div class="documentation">
      ${nxItem.documentationHtml}
    </div>
  </#if>

  <h2 class="toc">Contribution Descriptors</h2>
  <ul class="descriptors">
    <#list nxItem.descriptors as descriptor>
    <li><@javadoc descriptor true /></li>
    </#list>
  </ul>

  <#if extensions?size gt 0>
    <h2 class="toc">Existing Contributions</h2>
    <small>
      Contributions are presented in the same order as the registration order on this extension point.
      This order is displayed before the contribution name, in brackets.
    </small>
    <p>
      <form id="searchContributions">
        <input type="search" id="searchField" placeholder="Text in contributions" autofocus />
        <input type="submit" value="search" onclick="searchContrib($('#searchField').val()); return false;" />
        <span id="searchMatchResult"></span>
      </form>
    </p>
    <ul id="highlight-plugin" class="block-list">
      <#list extensions as contrib>
      <li id="${contrib.id}" class="block-item">
        <div class="searchableText">
          <span style="display:none">${contrib.component.bundle.fileName} ${contrib.component.xmlFileName}</span>
          <pre><code>${contrib.xml?xml}</code></pre>
        </div>
        <div class="block-title">
          (<#if contrib.registrationOrder??>${contrib.registrationOrder?string.computer}<#else>?</#if>)
          <a class="components" href="${Root.path}/${distId}/viewContribution/${contrib.id}">
            ${contrib.component.bundle.fileName}${contrib.component.xmlFileName}
          </a>
          &nbsp;
          <a class="override button" onclick="$.fn.clickButton(this)"
            href="${Root.path}/${distId}/viewComponent/${contrib.component.id}/override/?contributionId=${contrib.id}" target="_blank">
            Override
          </a>
        </div>
      </li>
      </#list>
    </ul>

  <#else>
    <h2 class="toc">Contributions</h2>
    No known contributions.
  </#if>

  <@tocTrigger />

</@block>

<@block name="footer_scripts">
  <script>
  function searchContrib(text) {

    $('#highlight-plugin').removeHighlight();
    $('#highlight-plugin').find('li').show();
    $('#searchMatchResult').html("");

    if (text.trim().length==0) {
      $('#searchMatchResult').html("empty search string!");
      return;
    }

    var elems = $('div.searchableText:contains("' + text +'")');
    if (elems.size()>0) {
      $('div.searchableText').highlight(text);
      $('#searchMatchResult').html(elems.size() + " matching contribution(s)");

      $('#highlight-plugin').find('li').hide();
      elems.each(function(i, elt) {
        console.log(elt);
        console.log($(elt).parent('li'));
        $(elt).parent('li').show();
      });
    } else {
      $('#searchMatchResult').html("no match found");
    }
  }
  </script>
</@block>

</@extends>
