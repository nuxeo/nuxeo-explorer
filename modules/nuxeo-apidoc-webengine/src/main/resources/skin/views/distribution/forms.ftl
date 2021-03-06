<@extends src="base.ftl">

<@block name="right">

  <h1>Distributions</h1>

  <table class="tablesorter distributions">
    <tr>
      <th>Name</th>
      <th>Aliases</th>
      <th>Version</th>
      <th>Creation date</th>
      <th>Release date</th>
      <th>Flags</th>
      <th>Actions</th>
    </tr>

    <#if Root.showRuntimeSnapshot()>
    <#assign rtSnap=Root.runtimeDistribution/>
    <tr>
      <td>
        <a class="button currentDistrib" href="${Root.path}/current/">${rtSnap.name}</a>
      </td>
      <td>
        <#if rtSnap.aliases?size gt 0>
          <div>
            <#list rtSnap.aliases as alias>
              <a class="button" href="${Root.path}/${alias}/">${alias?html}</a>
            </#list>
          </div>
        </#if>
      </td>
      <td>${rtSnap.version}</td>
      <td>${rtSnap.creationDate?datetime}</td>
      <td>-</td>
      <td><span class="sticker current">Current deployed distribution (live)</span></td>
      <td>
        <div id="saveBtn">
          <form method="POST" action="${Root.path}/save">
            <input type="button" value="Save" id="save"
              onclick="$('#stdSave').css('display','block');$('#saveBtn').css('display','none')">
            <input type="button" value="Save Partial Snapshot" id="savePartial"
              onclick="$('#extendedSave').css('display','block');$('#saveBtn').css('display','none')">
          </form>
          <a class="button" href="${Root.path}/current/json" onclick="$.fn.clickButton(this)" target="_blank">
            Export as json
          </a>
        </div>
        <div style="display:none" id="stdSave">
          <form method="POST" action="${Root.path}/save">
            <table>
              <tr>
                <td class="nowrap">Name</td>
                <td><input type="text" name="name" value="${rtSnap.name}"/></td>
              </tr>
              <tr>
                <td class="nowrap">Release Date</td>
                <td><input type="date" name="released" placeholder="yyyy-MM-dd" /></td>
              </tr>
              <tr>
                <td class="nowrap">Version</td>
                <td><span name="version">${rtSnap.version}</span></td>
              </tr>
            </table>
            <input type="hidden" name="source" value="admin">
            <input type="submit" value="Save" id="doSave" class="button primary" onclick="$.fn.clickButton(this)" />
            <input type="button" value="Cancel" id="save"
              onclick="$('#stdSave').css('display','none');$('#saveBtn').css('display','block')">
          </form>
        </div>
        <div style="display:none" id="extendedSave">
          <form method="POST" action="${Root.path}/saveExtended">
            <table>
              <tr>
                <td class="nowrap">Name</td>
                <td><input type="text" name="name" value="${rtSnap.name}"/></td>
              </tr>
              <tr>
                <td class="nowrap">Release Date</td>
                <td><input type="date" name="released" /></td>
              </tr>
              <tr>
                <td class="nowrap">Bundles</td>
                <td><textarea rows="4" cols="30" name="bundles"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Excluded Bundles</td>
                <td><textarea rows="4" cols="30" name="excludedBundles"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Nuxeo Packages</td>
                <td><textarea rows="4" cols="30" name="nuxeoPackages"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Excluded Nuxeo Packages</td>
                <td><textarea rows="4" cols="30" name="excludedNuxeoPackages"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">JAVA Packages Prefixes</td>
                <td><textarea rows="4" cols="30" name="javaPackagePrefixes"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap">Excluded JAVA Packages Prefixes</td>
                <td><textarea rows="4" cols="30" name="excludedJavaPackagePrefixes"></textarea></td>
              </tr>
              <tr>
                <td class="nowrap"><label for="checkAsPrefixes">Check Bundles and Packages as Prefixes</label></td>
                <td><input type="checkbox" name="checkAsPrefixes" checked /></td>
              </tr>
              <tr>
                <td class="nowrap"><label for="includeReferences">Include References</label></td>
                <td><input type="checkbox" name="includeReferences" /></td>
              </tr>
              <tr>
                <td class="nowrap">Version</td>
                <td><span name="version">${rtSnap.version}</span></td>
              </tr>
            </table>
            <input type="hidden" name="source" value="admin">
            <input type="submit" value="Save" id="doSaveExtended" class="button primary" onclick="$.fn.clickButton(this)" />
            <input type="button" value="Cancel" id="save"
              onclick="$('#extendedSave').css('display','none');$('#saveBtn').css('display','block')">
          </form>
        </div>

      </td>
    </tr>
    </#if>

    <#list Root.listPersistedDistributions() as distrib>
      <tr>
        <td>
          <a class="distrib button" href="${Root.path}/${distrib.key}/">${distrib.name}</a>
          <#if Root.isKeyDuplicated(distrib.key)>
            <div class="message error duplicateKey">
              Duplicate key detected: '${distrib.key}'
            </div>
          </#if>
        </td>
        <td>
          <#if distrib.aliases?size gt 0>
            <div>
              <#list distrib.aliases as alias>
                <a class="button" href="${Root.path}/${alias}/">${alias?html}</a>
              </#list>
            </div>
          </#if>
        </td>
        <td>${distrib.version}</td>
        <td>${distrib.creationDate?datetime}</td>
        <td>${distrib.releaseDate?datetime}</td>
        <td>
          <#if distrib.latestFT>
            <span class="sticker current">Latest FT</span>
          </#if>
          <#if distrib.latestLTS>
            <span class="sticker current">Latest LTS</span>
          </#if>
          <#if distrib.hidden>
            <span class="sticker current">Hidden</span>
          </#if>
        </td>
        <td>
          <p>
            <a class="button" href="${Root.path}/updateDistrib/${distrib.key}?distribDocId=${distrib.doc.id}" onclick="$.fn.clickButton(this)">
              Update
            </a>
            <a class="button" href="${Root.path}/delete/${distrib.key}?distribDocId=${distrib.doc.id}" onclick="if (confirm('Please confirm deletion')) {$.fn.clickButton(this); return true; } else { return false; }">
              Delete
            </a>
          </p>
          <p>
            <a class="button" href="${Root.path}/download/${distrib.key}" onclick="$.fn.clickButton(this)">
              Export as zip
            </a>
            <a class="button" href="${Root.path}/${distrib.key}/json" onclick="$.fn.clickButton(this)" target="_blank">
              Export as json
            </a>
          </p>
        </td>
      </tr>
    </#list>

  </table>

  <h1>Upload Distribution</h1>
  <div class="box">
    <p>You can use the form below to upload a distribution that has been exported as a zip:</p>
    <form method="POST" action="${Root.path}/uploadDistribTmp" enctype="multipart/form-data">
      <input type="file" name="archive" id="archive">
      <input type="hidden" name="source" value="admin">
      <input type="submit" value="Upload" id="upload" onclick="$.fn.clickButton(this)">
    </form>
  </div>

</@block>

</@extends>
