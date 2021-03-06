<@extends src="base.ftl">
<@block name="title">All Services</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${services?size} Services</h1>
<div class="tabscontent">

  <table id="contentTable" class="tablesorter">
  <thead>
    <tr>
      <th>
        <@tableFilterArea "service"/>
      </th>
    </tr>
  </thead>
  <tbody>
    <#list services as service>
    <tr>
      <td>
        <div>
          <h4><a title="Service Short Name" href="${Root.path}/${distId}/viewService/${service.id}" class="itemLink">${service.label}</a></h4>
          <div class="itemDetail">
            <span title="Service Name">${service.id}</span>
          </div>
       </div>
      </td>
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
