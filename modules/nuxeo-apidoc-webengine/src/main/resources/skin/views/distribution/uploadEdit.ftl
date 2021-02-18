<@extends src="base.ftl">

<@block name="left" />

<@block name="right">

  <div>
    The distribution zip you have uploaded contains <b>${snapObject.bundleIds?size} bundles</b>.
  </div>
  <div>
    You can edit some of the properties of the distribution before you validate the upload:<br/>
  </div>

  <form method="POST" action="${Root.path}/uploadDistribTmpValid">
    <table>
      <tr>
        <td class="label required">Title</td>
        <td>
          <input type="text" name="dc:title" value="${tmpSnap.title}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label required">Name</td>
        <td>
          <input type="text" name="nxdistribution:name" value="${tmpSnap.nxdistribution.name}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label required">Version</td>
        <td>
          <input type="text" name="nxdistribution:version" value="${tmpSnap.nxdistribution.version}" width="100%" />
        </td>
      </tr>
      <tr>
        <td class="label required">Key</td>
        <td>
          <input type="text" name="nxdistribution:key" value="${tmpSnap.nxdistribution.key}" width="100%" />
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <input type="hidden" name="distribDocId" value="${tmpSnap.id}" />
          <input type="hidden" name="source" value="${source}" />
          <input type="submit" value="Import bundles" id="doImport" onclick="$.fn.clickButton(this)" />
        </td>
      </tr>
    </table>
  </form>

</@block>

</@extends>
