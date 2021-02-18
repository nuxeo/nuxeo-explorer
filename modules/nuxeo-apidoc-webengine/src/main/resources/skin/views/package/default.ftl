<@extends src="base.ftl">
<@block name="title">Package ${nxItem.title}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Package <span class="componentTitle">${nxItem.title}</span> (${nxItem.name})</h1>

<div class="tabscontent">
  <@toc />

  <h2 class="toc">General Information</h2>
  <div class="info">
    <table class="listTable">
      <tr>
        <th>Id</th>
        <td id="packageId">${nxItem.id?html}</td>
      </tr>
      <tr>
        <th>Name</th>
        <td id="packageName">${nxItem.name?html}</td>
      </tr>
      <tr>
        <th>Version</th>
        <td id="packageVersion">${nxItem.version?html}</td>
      </tr>
      <#if marketplaceURL??>
      <tr>
        <th>Marketplace Link</th>
        <td><a id="marketplaceLink" href="${marketplaceURL}" target="_blank">${marketplaceURL}</a></td>
      </tr>
      </#if>
    </table>

  </div>

  <#if nxItem.dependencies?size gt 0 || nxItem.optionalDependencies?size gt 0 || nxItem.conflicts?size gt 0>
  <div id="alldependencies">
    <#if nxItem.dependencies?size gt 0>
    <h2 class="toc">Dependencies</h2>
    <ul id="dependencies">
      <#list nxItem.dependencies as dep>
        <#if dependencies[dep]??>
          <li><a class="packages" href="${Root.path}/${distId}/viewPackage/${dependencies[dep].name}">${dep}</a></li>
        <#else>
          <li><span class="packages">${dep}</li>
        </#if>
      </#list>
    </ul>
    </#if>
    <#if nxItem.optionalDependencies?size gt 0>
    <h2 class="toc">Optional Dependencies</h2>
    <ul id="optionalDependencies">
      <#list nxItem.optionalDependencies as dep>
        <#if optionalDependencies[dep]??>
          <li><a class="packages" href="${Root.path}/${distId}/viewPackage/${dep}">${dep}</a></li>
        <#else>
          <li><span class="packages">${dep}</li>
        </#if>
      </#list>
    </ul>
    </#if>
    <#if nxItem.conflicts?size gt 0>
    <h2 class="toc">Conflicts</h2>
    <ul id="conflicts">
      <#list nxItem.conflicts as dep>
        <#if conflicts[dep]??>
          <li><a class="packages" href="${Root.path}/${distId}/viewPackage/${dep}">${dep}</a></li>
        <#else>
          <li><span class="packages">${dep}</li>
        </#if>
      </#list>
    </ul>
    </#if>
  </div>
  </#if>

  <h2 class="toc">Bundles</h2>
  <div id="bundles">
  <#if nxItem.bundles?size gt 0>
    <ul>
      <#list nxItem.bundles as bundle>
        <#if bundles[bundle]??>
          <li><a class="bundles" href="${Root.path}/${distId}/viewBundle/${bundle}">${bundle}</a></li>
        <#else>
          <li><span class="bundles">${bundle}</span></li>
        </#if>
      </#list>
    </ul>
  <#else>
    <div>No bundles.</div>
  </#if>
  </div>

  <#if components?size gt 0>
    <h2 class="toc">Components</h2>
    <div id="components">
      <ul>
        <#list components as component>
        <li><a class="components" href="${Root.path}/${distId}/viewComponent/${component.id}">${component.id}</a></li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if services?size gt 0>
    <h2 class="toc">Services</h2>
    <div id="services">
      <ul>
        <#list services as service>
        <li><a class="services" href="${Root.path}/${distId}/viewService/${service.id}">${service.label}</a></li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if extensionpoints?size gt 0>
    <h2 class="toc">Extension Points</h2>
    <div id="extensionpoints">
      <ul>
        <#list extensionpoints as xp>
        <li><a class="extensions" href="${Root.path}/${distId}/viewExtensionPoint/${xp.id}">${xp.id}</a></li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if contributions?size gt 0>
    <h2 class="toc">Contributions</h2>
    <div id="contributions">
      <ul>
        <#list contributions as contribution>
        <li><a class="contributions" href="${Root.path}/${distId}/viewContribution/${contribution.id}">${contribution.id}</a></li>
        </#list>
      </ul>
    </div>
  </#if>

  <h2 class="toc">Exports</h2>
  <ul class="exports">
    <li>
      <a href="${Root.path}/${distId}/json?nuxeoPackages=${nxItem.name}&pretty=true">Json Export</a>
      <small>Default Json serialization</small>
    </li>
    <#list exporters as exporter>
    <li>
      <a href="${Root.path}/${distId}/export/${exporter.name}?nuxeoPackages=${nxItem.name}&pretty=true">${exporter.title?html}</a>
      <#if exporter.description?has_content><small>${exporter.description?html}</small></#if>
    </li>
    </#list>
  </ul>

  <@tocTrigger />

</div>
</@block>
</@extends>
