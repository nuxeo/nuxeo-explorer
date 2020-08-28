<#setting url_escaping_charset="ISO-8859-1">
<@extends src="base.ftl">
<@block name="title">Bundle Group ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">
<#assign nestedLevel=0/>

<h1>Bundle Group <span class="componentTitle">${nxItem.name}</span></h1>
<#if nxItem.parentGroup??>
  <div class="include-in">In bundle group <a href="${Root.path}/${distId}/viewBundleGroup/${nxItem.parentGroup.id}">${nxItem.parentGroup.name}</a></div>
</#if>

<div class="tabscontent">
  <@toc />

  <#if nxItem.readmes?size gt 0>
    <h2 class="toc">Documentation</h2>
    <div class="documentation">
      <ul class="block-list">
        <#list nxItem.readmes as readme>
          <li class="block-item">
            <div class="block-title dark">
              ${readme.filename}
            </div>
            <div class="block-content">
              ${readmes[readme?index]}
            </div>
          </li>
        </#list>
      </ul>
    </div>
  </#if>

  <#if nxItem.subGroups?size gt 0>
  <h2 class="toc">Bundle Subgroups</h2>
  <ul class="subbroups">
    <#list nxItem.subGroups as subGroup>
    <li>
      <a href="${Root.path}/${distId}/viewBundleGroup/${subGroup.name}">${subGroup.name}</a>
    </li>
    </#list>
  </ul>
  </#if>

  <#if nxItem.bundleIds?size gt 0>
  <h2 class="toc">Bundles</h2>
  <ul class="groupbundles">
    <#list nxItem.bundleIds as bundleId>
    <li>
      <a href="${Root.path}/${distId}/viewBundle/${bundleId}">${bundleId}</a>
    </li>
    </#list>
  </ul>
  </#if>

  <@tocTrigger />

</div>

</@block>
</@extends>
