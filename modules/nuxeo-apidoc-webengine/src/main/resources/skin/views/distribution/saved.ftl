<@extends src="base.ftl">

<@block name="left" />

<@block name="right">

  <h1 class="message success">Distribution Snapshot saved successfully</h1>
  <div>
    <#assign view=This.getRedirectViewPostUpload(source) />
    <a href="${Root.path}/${view}" class="button">Continue</a>
  </div>

</@block>

</@extends>
