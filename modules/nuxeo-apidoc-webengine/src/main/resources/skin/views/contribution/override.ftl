<?xml version="1.0"?>
<component name="${contribution.component.id}.override">

  <require>${contribution.component.id}</require>

<#if ep.documentation?has_content>
  <documentation>
${ep.documentation}
  </documentation>
</#if>

  <extension target="${ep.componentId}" point="${ep.name}">

  <#list contribution.contributionItems as contributionItem>
    <#if !selectedContribs?? || selectedContribs?seq_contains(contributionItem.id)>
    ${contributionItem.rawXml}
    </#if>
  </#list>

  </extension>

</component>
