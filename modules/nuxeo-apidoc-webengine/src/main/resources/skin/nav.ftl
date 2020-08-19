<#if Root.currentDistribution!=null>

<#assign navPoint=Root.getNavigationPoint()/>

<#if !Root.isEmbeddedMode()>

<div class="nav-box">
<ul>
  <li <#if navPoint=="listExtensionPoints">class="selected"</#if> >
    <a class="extensions" href="${Root.path}/${distId}/listExtensionPoints">Extension points</a>
  </li>

  <li <#if navPoint=="listContributions">class="selected"</#if> >
  <a class="contributions" href="${Root.path}/${distId}/listContributions">Contributions</a>
  </li>

  <li <#if navPoint=="listServices">class="selected"</#if> >
    <a class="services" href="${Root.path}/${distId}/listServices">Services</a>
  </li>

  <li <#if navPoint=="listOperations">class="selected"</#if> >
    <a class="operations" href="${Root.path}/${distId}/listOperations">Operations</a>
  </li>

  <li <#if navPoint=="listComponents">class="selected"</#if> >
    <a class="components" href="${Root.path}/${distId}/listComponents">Components</a>
  </li>

  <li <#if navPoint=="listBundles">class="selected"</#if> >
    <a class="bundles" href="${Root.path}/${distId}/listBundles">Bundles</a>
  </li>

  <li <#if navPoint=="listPackages">class="selected"</#if> >
    <a class="packages" href="${Root.path}/${distId}/listPackages">Packages</a>
  </li>

  <#list Root.getPluginMenu() as plugin>
    <li <#if navPoint==plugin.getHomeView()>class="selected"</#if> >
      <a class="plugin ${plugin.getStyleClass()}" href="${Root.path}/${distId}/${plugin.getViewType()}/${plugin.getHomeView()}">${plugin.getLabel()}</a>
    </li>
  </#list>

</ul>
</div>

</#if>

</#if>
