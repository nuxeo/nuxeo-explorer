<@extends src="base.ftl">

<@block name="right">
<#include "/docMacros.ftl">

<h1> Browsing Distribution '${Root.currentDistribution.key}' </h1>

<div class="tabscontent">
  <@toc />

  <h2 class="toc">Browse</h2>
  <ul>
    <li>
      <a class="bundles" href="${Root.path}/${distId}/listBundleGroups">Bundle Groups</a>
      <span class="chip">${stats.bundlegroups}</span>
      <ul>
        <li>
          <a class="bundles" href="${Root.path}/${distId}/listBundles">Bundles</a>
          <span class="chip">${stats.bundles}</span>
          <ul>
            <li>
              <a class="components" href="${Root.path}/${distId}/listComponents">Components</a>
              <span class="chip">${stats.components}</span>
              <ul>
                <li>
                  <a class="services" href="${Root.path}/${distId}/listServices">Services</a>
                  <span class="chip">${stats.services}</span>
                </li>
                <li>
                  <a class="extensions" href="${Root.path}/${distId}/listExtensionPoints">Extension Points</a>
                  <span class="chip">${stats.xps}</span>
                </li>
                <li>
                  <a class="contributions" href="${Root.path}/${distId}/listContributions">Contributions</a>
                  <span class="chip">${stats.contribs}</span>
                </li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>
    </li>
    <li>
      <a class="operations" href="${Root.path}/${distId}/listOperations">Operations</a>
      <span class="chip">${stats.operations}</span>
    </li>
    <li>
      <a class="packages" href="${Root.path}/${distId}/listPackages">Packages</a>
      <span class="chip">${stats.packages}</span>
    </li>
    <#list Root.getPluginMenu() as plugin>
      <li>
        <a class="${plugin.getStyleClass()}" href="${Root.path}/${distId}/${plugin.getViewType()}/${plugin.getHomeView()}">${plugin.getLabel()}</a>
      </li>
    </#list>
  </ul>

  <h2 class="toc">Exports</h2>
  <ul class="exports">
    <li>
      <a href="${Root.path}/${distId}/json?pretty=true">Json Export</a>
      <small>Default Json serialization</small>
    </li>
    <#list exporters as exporter>
    <li>
      <a href="${Root.path}/${distId}/export/${exporter.name}?pretty=true">${exporter.title?html}</a>
      <#if exporter.description?has_content><small>${exporter.description?html}</small></#if>
    </li>
    </#list>
  </ul>

  <@tocTrigger />

</div>

</@block>

</@extends>
