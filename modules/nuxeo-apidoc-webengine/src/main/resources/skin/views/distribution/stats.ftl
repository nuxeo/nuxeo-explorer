<@extends src="base.ftl">

<@block name="title">Nuxeo Platform Explorer - Statistics</@block>

<@block name="right">

  <h1>Statistics</h1>

  <div class="tabscontent">
    <div class="stats">
      <div class="stats distributions">
        <form>
          <input type="button" class="submitSelection" value="Submit" />
          <table id="distribTable" class="tablesorter">
            <thead>
              <tr>
                <th class="select">
                  <input type="checkbox" id="selectAll">
                </th>
                <th class="header">
                  Distribution Name
                </th>
                <th class="header">
                  Version
                </th>
              </tr>
            </thead>
            <tbody>
              <#list Root.listPersistedDistributions() as distrib>
              <tr>
                <td class="select">
                  <input type="checkbox" name="distrib" id="${distrib.key}" value="${distrib.key}">
                </td>
                <td>
                  <label for="${distrib.key}">${distrib.name}</label>
                </td>
                <td>
                  <label for="${distrib.key}">${distrib.version}</label>
                </td>
              </tr>
              </#list>
            </tbody>
          </table>
          <input type="button" class="submitSelection" value="Submit" />
        </form>
      </div>
      <div class="stats statcharts">
        <div id="errors" class="chartError"></div>
        <div id="feedback" class="chartHeader"></div>
        <div class="charts">
          <div class="chart main" id='codeTypeStats'></div>
          <div class="chart details" id='codeTypeStatsDetails'></div>
          <div style="clear:both;"></div>
        </div>
        <div class="charts">
          <div class="chart main" id='xpStats'></div>
          <div class="chart details" id='xpStatsDetails'></div>
          <div style="clear:both;"></div>
        </div>
        <div class="charts">
          <div class="chart main" id='studioStats'></div>
          <div class="chart details" id='studioStatsDetails'></div>
          <div style="clear:both;"></div>
        </div>
      </div>
      <div style="clear:both;"></div>
    </div>
  </div>

</@block>

<@block name="footer_scripts">
  <script>
    const rootURL = '${Root.path}';
    const exporterName = 'jsonContributionStats';
    const loadingDataText = "Loading data";
    const noDataText = "No data";

    const render = () => {
      $("#feedback").html(loadingDataText);
      $("#errors").html('');

      var checkboxes = document.querySelectorAll('input[name="distrib"]:checked');
      var distribKeys = [...checkboxes].map(c => c.value);
      var total = distribKeys.length;
      if (total == 0) {
        $("#feedback").html(noDataText);
        $("#codeTypeStats").html('');
        $("#xpStats").html('');
        $("#studioStats").html('');
        $("#codeTypeStatsDetails").html('');
        $("#xpStatsDetails").html('');
        $("#studioStatsDetails").html('');
      } else {
        var allStats = [];
        distribKeys.map(d => Plotly.d3.json(rootURL + '/' + d + '/export/' + exporterName, (err, data) => renderDistrib(err, data, d, total, allStats)));
      }
    };

    const renderDistrib = (error, rawData, distribKey, total, allStats) => {
      var count = allStats.length + 1;
      $("#feedback").html('Loading data ' + count + '/' + total);

      if (error || !rawData) {
        allStats.push([]);
        $("#errors").append('<div>Error retrieving data for ' + distribKey + '</div>');
        return;
      }
      var stats = rawData.stats;
      stats.forEach(stat => stat['distribKey'] = distribKey);
      allStats.push(stats);

      if (allStats.length == total) {
        $("#codeTypeStatsDetails").html('');
        $("#xpStatsDetails").html('');
        $("#studioStatsDetails").html('');
        var combinedStats = Array.prototype.concat.apply([], allStats);
        $("#codeTypeStats").plotlyCodeTypeChart({rootURL: rootURL, stats: combinedStats, title: 'Contributions by Code Type'});
        $("#xpStats").plotlyXPChart({rootURL: rootURL, stats: combinedStats, title: 'Contributions by Target Extension Point'});
        $("#studioStats").plotlyStudioChart({rootURL: rootURL, stats: combinedStats, title: 'Contributions by Studio Source'});
        $("#feedback").html('');
      }
    }

    $('#selectAll').click(function () {
      $('#distribTable input[type="checkbox"]').prop('checked', this.checked);
    });
    $('.submitSelection').click(() => render());

    $(document).ready(function() {
      $("#distribTable").tablesorter({
        sortList: [[1,0]],
        headers: {0: {sorter: false}},
        widgets: ['zebra'],
        cancelSelection: false
      });
    });

    window.onload = render;
  </script>
</@block>

</@extends>
