<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle ${nxItem.id}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Bundle <span class="componentTitle">${nxItem.id}</span></h1>
<div class="include-in">In bundle group <a href="${Root.path}/${distId}/viewBundleGroup/${nxItem.bundleGroup.id}">${nxItem.bundleGroup.name}</a></div>

<div class="tabscontent">
  <@toc />

  <#if readme?has_content || parentReadme?has_content>
    <h2 class="toc">Documentation</h2>
    <div class="documentation">
      <ul class="block-list">
        <#if readme?has_content>
          <li class="block-item">
            <div class="block-title dark">
              ${nxItem.readme.filename}
            </div>
            <div class="block-content">
              ${readme}
            </div>
          </li>
        </#if>
        <#if parentReadme?has_content>
          <li class="block-item">
            <div class="block-title dark">
              Parent Documentation: ${nxItem.parentReadme.filename}
            </div>
            <div class="block-content">
              ${parentReadme}
            </div>
          </li>
        </#if>
      </ul>
    </div>
  </#if>

  <#if nxItem.requirements?size gt 0>
    <h2 class="toc">Requirements</h2>
    <ul class="nolist" id="requirements">
      <#list nxItem.requirements as req>
        <#if requirements[req]??>
          <li><a class="tag bundles" href="${Root.path}/${distId}/viewBundle/${req}">${req}</a></li>
        <#else>
          <li><span class="bundles">${req}</li>
        </#if>
      </#list>
    </ul>
  </#if>

  <#if nxItem.minResolutionOrder??>
    <h2 class="toc">Resolution Order</h2>
    <#if nxItem.minResolutionOrder = nxItem.maxResolutionOrder>
      <div id="resolutionOrder" class="resolutionOrder">
        ${nxItem.minResolutionOrder?string.computer}
      </div>
      <small id="resolutionOrderHelp">
        The resolution order represents the order in which this bundle's component has been resolved by the Nuxeo Runtime framework.
        <br />
        You can influence this order by adding "require" tags in the component declaration, to make sure it is resolved after another component.
      </small>
    <#else>
      <div id="resolutionOrder" class="resolutionOrder">
        [${nxItem.minResolutionOrder?string.computer}, ${nxItem.maxResolutionOrder?string.computer}]
      </div>
      <small id="resolutionOrderHelp">
        The resolution order represents the order in which components have been resolved by the Nuxeo Runtime framework.
        This range represents the minimal and maximal orders for this bundle's components.
        <br />
        You can influence this order by adding "require" tags in the component declaration, to make sure it is resolved after another component.
      </small>
    </#if>
  </#if>

  <h2 class="toc">Components</h2>
  <#if nxItem.components?size == 0>
    No components.
  <#else>
    <ul class="nolist">
      <#list nxItem.components as component>
      <li><a class="tag components" href="${Root.path}/${distId}/viewComponent/${component.name}">${component.name}</a></li>
      </#list>
    </ul>
  </#if>

  <#if nxItem.packages?size gt 0>
    <h2 class="toc">Packages</h2>
    <ul class="nolist packages">
      <#list nxItem.packages as pkg>
      <li><a class="tag packages" href="${Root.path}/${distId}/viewPackage/${pkg}">${pkg}</a></li>
      </#list>
    </ul>
  </#if>

  <h2 class="toc">Maven Artifact</h2>
  <table class="listTable">
    <tr><th>File</th><td>${nxItem.fileName}</td></tr>
    <tr><th>Group Id</th><td>${nxItem.groupId}</td></tr>
    <tr><th>Artifact Id</th><td>${nxItem.artifactId}</td></tr>
    <tr><th>Version</th><td>${nxItem.artifactVersion}</td></tr>
  </table>

  <#if nxItem.manifest?has_content>
    <h2 class="toc">Manifest</h2>
    <div>
      <pre><code>${nxItem.manifest}</code></pre>
    </div>
  </#if>

  <h2 class="toc">Exports</h2>
  <ul class="exports">
    <li>
      <a href="${Root.path}/${distId}/json?bundles=${nxItem.id}&pretty=true">Json Export</a>
      <small>Default Json serialization</small>
    </li>
    <#list exporters as exporter>
    <li>
      <a href="${Root.path}/${distId}/export/${exporter.name}?bundles=${nxItem.id}&pretty=true">${exporter.title?html}</a>
      <#if exporter.description?has_content><small>${exporter.description?html}</small></#if>
    </li>
    </#list>
  </ul>

  <@tocTrigger />

</div>

</@block>
</@extends>
