(function ($) {

  const NUMBER_XP_THRESHOLD = 10;

  const PERCENT_DISPLAY_THRESHOLD = 1;

  const sum = (a, b) => a + b;

  $.fn.plotlyCodeTypeChart = function(options) {
    var settings = $.extend({title: ''}, options);

    var byCodeType = settings.stats.reduce(function(map, stat) {
      var type = stat.codeType;
      map[type] = map[type] || [];
      map[type].push(stat);
      return map;
    }, {});
    var pieData = [byCodeType['XML'] || [], byCodeType['JAVA'] || [], byCodeType['JAVALIKE'] || [], byCodeType['SCRIPTING'] || []];
    var values = pieData.map(l => l.map(s => s.numberOfContributions).reduce(sum, 0));
    var total = values.reduce(sum, 0);

    var data = [{
      values,
      labels: ['XML', 'Java', 'Java Like', 'Scripting'],
      customdata: pieData,
      type: 'pie'
    }];

    var chartDivId = this.attr('id');
    var div = document.getElementById(chartDivId);
    div.textContent = '';
    Plotly.newPlot(chartDivId, data, getPieChartLayout(settings.title), getPieChartConfig());
    div.on('plotly_click', data => showDetails(data, total, settings.rootURL, chartDivId + 'Details'));
  }

  $.fn.plotlyXPChart = function(options) {
    var settings = $.extend({title: ''}, options);

    var total = 0;
    var dict = settings.stats.reduce((map, stat, index) => {
      var id = stat.targetExtensionPointId;
      total += stat.numberOfContributions;
      map[id] = map[id] || { stats: [], count: 0};
      map[id].stats.push(stat);
      map[id].count += stat.numberOfContributions;
      return map;
    }, {});
    var array = Object.keys(dict).map(function(key) {
      return [key, dict[key]];
    });
    array.sort(function(first, second) {
      return second[1].count - first[1].count;
    });

    var values = [], labels = [], pieData = [], positions = [];
    var mvalues = [], mlabels = [];
    array.forEach((item, index) => {
      const [key, value] = item;
      values.push(value.count);
      var label = getLabel(key);
      labels.push(label);
      pieData.push(value.stats);
      var isNotSmall = getPerCent(value.count, total) > PERCENT_DISPLAY_THRESHOLD;
      var ismain = isNotSmall && index <= NUMBER_XP_THRESHOLD;
      positions.push(ismain ? 'auto': 'none');
      if (ismain) {
        mvalues.push(value.count);
        mlabels.push(label);
      }
    });

    var data = [{
      values: mvalues,
      labels: mlabels,
      customdata: pieData,
      type: 'pie',
    }];
    var layout =  getPieChartLayout(settings.title);

    var showsAll = mvalues.length == values.length;
    if (!showsAll) {
      data.push({
        values,
        labels,
        textposition: positions,
        visible: false,
        customdata: pieData,
        type: 'pie',
      });
      Object.assign(layout, {
        updatemenus: [{
          type: 'buttons',
          active: -1,
          buttons: [{
            args: [
              { visible: [ false, true ] },
            ],
            args2: [
              { visible: [ true, false ] },
            ],
            label: 'Show All',
            method: 'update'
          }]
        }]
      });
    }

    var chartDivId = this.attr('id');
    var div = document.getElementById(chartDivId);
    div.textContent = '';
    Plotly.newPlot(chartDivId, data, layout, getPieChartConfig());
    div.on('plotly_click', data => showDetails(data, total, settings.rootURL, chartDivId + 'Details'));
  }

  $.fn.plotlyStudioChart = function(options) {
    var settings = $.extend({title: ''}, options);

    var studio = [], notStudio = [];
    settings.stats.forEach(stat => {
      (stat.fromStudio ? studio: notStudio).push(stat);
    });
    var pieData = [studio, notStudio];
    var values = pieData.map(s => s.map(e => e.numberOfContributions).reduce(sum, 0));
    var total = values.reduce(sum, 0);

    var data = [{
      values,
      labels: ['From Studio', 'Not From Studio'],
      customdata: pieData,
      type: 'pie'
    }];

    var chartDivId = this.attr('id');
    var div = document.getElementById(chartDivId);
    div.textContent = '';
    Plotly.newPlot(chartDivId, data, getPieChartLayout(settings.title), getPieChartConfig());
    div.on('plotly_click', data => showDetails(data, total, settings.rootURL, chartDivId + 'Details'));
  }

  function getPieChartLayout(title) {
    return {
      title,
      grid: { rows: 1, columns: 2 },
    };
  }

  function getPieChartConfig() {
    return {
      responsive: true,
      displaylogo: false,
      modeBarButtonsToRemove: ['hoverClosestPie', 'sendDataToCloud', 'toImage']
    };
  }

  function showDetails(clickData, total, rootURL, chartDetailsDivId) {
    var point = clickData.points[0];
    var tableStats = point.fullData.customdata[point.i];
    var label = point.label;

    var div = document.getElementById(chartDetailsDivId);
    div.textContent = '';

    var table = document.createElement('table');
    table.classList.add('tablesorter');
    table.classList.add('chart');

    var thead = table.createTHead();
    var theadRow = thead.insertRow();
    insertHeadCell(theadRow, 'Extension');
    insertHeadCell(theadRow, 'Target Extension Point');
    insertHeadCell(theadRow, 'Number of Contributions');
    var totalPercent = getPerCent(point.value, total).toFixed(2);
    insertHeadCell(theadRow, `${totalPercent}%`);

    var tbody = table.createTBody();
    tableStats.forEach(stat => insertRow(tbody, stat, total, rootURL));

    var headerDiv = document.createElement('div');
    headerDiv.classList.add('chartHeader');
    var headerLabel = document.createElement('span');
    headerLabel.appendChild(document.createTextNode('Selection: '+ label));
    headerDiv.appendChild(headerLabel);
    var clearButton = document.createElement('input');
    clearButton.setAttribute('type', 'button');
    clearButton.setAttribute('value', 'Clear');
    clearButton.addEventListener('click', () => {document.getElementById(chartDetailsDivId).textContent = '';});
    headerDiv.appendChild(clearButton);

    div.appendChild(headerDiv);
    div.appendChild(table);

    $(table).tablesorter({sortList: [[0,0]], widgets: ['zebra'], cancelSelection: false});
  }

  function insertHeadCell(tr, text) {
    var el = document.createElement('b');
    el.appendChild(document.createTextNode(text));
    var th = document.createElement('th');
    th.classList.add('header');
    th.appendChild(el);
    tr.appendChild(th);
  }

  function insertRow(tbody, stat, total, rootURL) {
    var tr = tbody.insertRow();

    var id = stat.extensionId;
    var statRootURL = rootURL;
    if (stat.distribKey) {
      statRootURL = `${rootURL}/${stat.distribKey}`;
    }
    insertCellLink(tr, `${statRootURL}/viewContribution/${id}`, getLabel(id));
    var xpid = stat.targetExtensionPointId;
    if (stat.targetExtensionPointPresent) {
      insertCellLink(tr, `${statRootURL}/viewExtensionPoint/${xpid}`, getLabel(xpid));
    } else {
      insertCellText(tr, xpid);
    }
    insertCellText(tr, stat.numberOfContributions);
    insertCellText(tr, getPerCent(stat.numberOfContributions, total).toFixed(2));
  }

  function insertCellLink(tr, href, text) {
    var el = document.createElement('a');
    el.setAttribute('href', href);
    el.setAttribute('target', '_blank');
    el.appendChild(document.createTextNode(text));
    tr.insertCell().appendChild(el);
  }

  function insertCellText(tr, text) {
    tr.insertCell().appendChild(document.createTextNode(text));
  }

  function getLabel(id) {
    return id.replace(/^.*\.(.*)$/, '$1');
  }

  function getPerCent(value, total) {
    return (100 * value) / total;
  }

})(jQuery);
