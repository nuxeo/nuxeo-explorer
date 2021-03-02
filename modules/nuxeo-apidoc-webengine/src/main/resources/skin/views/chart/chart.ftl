<#if exporter.name == 'jsonContributionStats'>

  <div class="chartTitle">
    <a href="${Root.path}/${distId}/export/${exporter.name}?${filter}" target="_blank">Raw Data</a>
  </div>
  <div class="charts">
    <h3>${exporter.title} - Code Type</h3>
    <div class="chart main" id='codeTypeStats'>No data</div>
    <div class="chart details" id='codeTypeStatsDetails'></div>
    <div style="clear:both;"></div>
  </div>
  <div class="charts">
    <h3>${exporter.title} - Target Extension Points</h3>
    <div class="chart main" id='xpStats'>No data</div>
    <div class="chart details" id='xpStatsDetails'></div>
    <div style="clear:both;"></div>
  </div>
  <div class="charts">
    <h3>${exporter.title} - Studio Source</h3>
    <div class="chart main" id='studioStats'>No data</div>
    <div class="chart details" id='studioStatsDetails'></div>
    <div style="clear:both;"></div>
  </div>
  <script>
    $(document).ready(function() {
      const rootURL = '${Root.path}/${distId}';
      const dataURL = rootURL + '/export/${exporter.name}?${filter}';
      Plotly.d3.json(dataURL, function (error, rawData) {
        if (error || !rawData) {
          return;
        }
        var stats = rawData.stats;
        if (stats === undefined || stats.length == 0) {
          return;
        }
        $("#codeTypeStats").plotlyCodeTypeChart({rootURL: rootURL, stats: stats});
        $("#xpStats").plotlyXPChart({rootURL: rootURL, stats: stats});
        $("#studioStats").plotlyStudioChart({rootURL: rootURL, stats: stats});
      });
    });
  </script>

</#if>
