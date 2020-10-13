<?xml version="1.0"?>
<component name="${component.id}.override">

  <require>${component.id}</require>

<#if component.documentation?has_content>
  <documentation>
${component.documentation}
  </documentation>
</#if>
<#if contribution??>
  ${contribution.xml}
<#else>
  <#list component.extensions as contribution>
  <extension target="${contribution.targetComponentName}" point="${contribution.extensionPoint}">
    <#list contribution.contributionItems as contributionItem>
    ${contributionItem.rawXml}
    </#list>
  </extension>
  </#list>
</#if>

</component>
