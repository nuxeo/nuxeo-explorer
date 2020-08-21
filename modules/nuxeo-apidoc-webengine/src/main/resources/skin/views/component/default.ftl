<@extends src="base.ftl">
<@block name="title">Component ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Component <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in bundles">In bundle <a href="${Root.path}/${distId}/viewBundle/${nxItem.bundle.id}">${nxItem.bundle.id}</a></div>

<div class="tabscontent">
  <@toc />

  <#if nxItem.documentationHtml?has_content>
    <h2 class="toc">Documentation</h2>
    <div class="documentation">
      ${nxItem.documentationHtml}
    </div>
  </#if>

  <#if nxItem.requirements?size gt 0>
    <h2 class="toc">Requirements</h2>
    <ul class="nolist" id="requirements">
      <#list nxItem.requirements as req>
        <#if requirements[req]??>
          <li><a class="tag components" href="${Root.path}/${distId}/viewComponent/${req}">${req}</a></li>
        <#else>
          <li><span class="components">${req}</li>
        </#if>
      </#list>
    </ul>
  </#if>

  <h2 class="toc">Registration Order</h2>
  <div id="registrationOrder">
    ${nxItem.registrationOrder}
  </div>
  <small id="registrationOrderHelp">
    The registration order represents the order in which this component has been deployed by the Nuxeo Runtime framework.
    <br />
    You can influence this order by adding "require" tags in your component declaration, to make sure it is deployed after another component.
  </small>

  <#if !nxItem.xmlPureComponent>
    <h2 class="toc">Implementation</h2>
    <div class="implementation">
      <@javadoc nxItem.componentClass true />
    </div>
  </#if>

  <#if nxItem.services?size gt 0>
  <h2 class="toc">Services</h2>
  <ul class="nolist">
    <#list nxItem.services as service>
    <li><a class="tag services" href="${Root.path}/${distId}/viewService/${service.id}">${service.id}</a></li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.extensionPoints?size gt 0>
  <h2 class="toc">Extension Points</h2>
  <ul class="nolist">
    <#list nxItem.extensionPoints as ep>
    <li><a class="tag extensions" href="${Root.path}/${distId}/viewExtensionPoint/${ep.id}">${ep.name}</a></li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.extensions?size gt 0>
  <h2 class="toc">Contributions</h2>
  <ul class="nolist">
    <#list nxItem.extensions as ex>
    <li><a class="tag contributions" href="${Root.path}/${distId}/viewContribution/${ex.id?url}">${ex.id}</a></li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.xmlFileContent?has_content>
    <h2 class="toc">XML Source</h2>
    <div id="xmlSource">
      <pre><code>${nxItem.xmlFileContent?html}</code></pre>
    </div>
  </#if>

  <@tocTrigger />

</div>

</@block>
</@extends>
