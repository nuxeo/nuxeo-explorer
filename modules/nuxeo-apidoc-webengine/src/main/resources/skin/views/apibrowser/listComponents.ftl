<@extends src="base.ftl">
<@block name="title">All Components</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${javaComponents?size + xmlComponents?size} Components</h1>

<div class="tabscontent">

  <table id="contentTable" class="tablesorter">
  <thead>
    <tr>
      <th>
        <@tableFilterArea "component"/>
      </th>
      <th>
        Resolution Order
      </th>
      <th>
        Declared Start Order
      </th>
    </tr>
  </thead>
  <tbody>
    <#list javaComponents as component>
    <tr>
      <td>
        <div>
          <h4><a title="Component Label" href="${Root.path}/${distId}/viewComponent/${component.id}" class="itemLink">${component.label}</a></h4>
          <div class="itemDetail">
            <span title="Component Type" class="sticker">Java</span>
            <span title="Component ID">${component.id}</span>
          </div>
      </td>
      <td>
        <#if component.order??>
          ${component.order?string.computer}
        </#if>
      </td>
      <td>
        <#if component.additionalOrder??>
          ${component.additionalOrder?string.computer}
        </#if>
      </td>
    </tr>
    </#list>
    <#list xmlComponents as component>
    <tr>
      <td>
        <div>
          <h4><a href="${Root.path}/${distId}/viewComponent/${component.id}" class="itemLink">${component.label}</a></h4>
          <div class="itemDetail">
            <span title="Component Type" class="sticker">XML</span>
            <span title="Component ID">${component.id}</span>
          </div>
        </div>
      </td>
      <td>
        <#if component.order??>
          ${component.order?string.computer}
        </#if>
      </td>
      <td></td>
    </tr>
    </#list>
  </tbody>
  </table>

</div>
</@block>

<@block name="footer_scripts">
  <@tableSortFilterScript "#contentTable" "[0,0]" />
</@block>

</@extends>
