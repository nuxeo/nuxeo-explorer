<#if exporter.name == 'jsonContributionStats'>

  <#assign dataURL="${Root.path}/${distId}/export/${exporter.name}?${filter}" />
  <#assign loadingDataText="Loading data" />
  <a href="${dataURL}" target="_blank">Raw Data</a>
  <div class="charts">
    <h3>${exporter.title} - Code Type</h3>
    <div class="chart main" id='codeTypeStats'>${loadingDataText}</div>
    <div class="chart details" id='codeTypeStatsDetails'></div>
    <div style="clear:both;"></div>
  </div>
  <div class="charts">
    <h3>${exporter.title} - Target Extension Points</h3>
    <div class="chart main" id='xpStats'>${loadingDataText}</div>
    <div class="chart details" id='xpStatsDetails'></div>
    <div style="clear:both;"></div>
  </div>
  <div class="charts">
    <h3>${exporter.title} - Studio Source</h3>
    <div class="chart main" id='studioStats'>${loadingDataText}</div>
    <div class="chart details" id='studioStatsDetails'></div>
    <div style="clear:both;"></div>
  </div>
  <script>
    $(document).ready(function() {
      const rootURL = '${Root.path}/${distId}';
      const dataURL = rootURL + '/export/${exporter.name}?${filter}';
      const errorText = "<span class='chartError'>Error retrieving data</span>";
      const noDataText = "No data";
      Plotly.d3.json(dataURL, function (error, rawData) {
        if (error || !rawData) {
          $("#codeTypeStats").html(errorText);
          $("#xpStats").html(errorText);
          $("#studioStats").html(errorText);
          return;
        }
        var stats = rawData.stats;
        if (stats.length == 0) {
          $("#codeTypeStats").html(noDataText);
          $("#xpStats").html(noDataText);
          $("#studioStats").html(noDataText);
          return;
        }
        $("#codeTypeStats").plotlyCodeTypeChart({rootURL: rootURL, stats: stats});
        $("#xpStats").plotlyXPChart({rootURL: rootURL, stats: stats});
        $("#studioStats").plotlyStudioChart({rootURL: rootURL, stats: stats});
      });
    });
  </script>

</#if>
