(function () {
  const script = document.currentScript;
  if (!script) {
    return;
  }

  const {
    chart: chartJson,
    coinId,
    vsCurrency,
    defaultDays,
    tradingviewSymbol,
  } = script.dataset;

  let tradingViewLoader = null;

  const canvas = document.getElementById('coinChart');
  const chartRangeSelector = document.querySelector('.chart-range-selector');

  if (tradingviewSymbol) {
    initializeTradingView(tradingviewSymbol, {
      onFallback: () => initializeChartJs(chartJson, coinId, vsCurrency, defaultDays),
    });
  } else {
    initializeChartJs(chartJson, coinId, vsCurrency, defaultDays);
  }

  function initializeTradingView(symbol, { onFallback } = {}) {
    const widgetContainer = document.getElementById('tradingviewWidget');
    const containerWrapper = document.getElementById('tradingviewWidgetContainer');

    if (!widgetContainer || !containerWrapper) {
      console.warn('TradingView widget container가 없습니다. Chart.js 차트로 대체합니다.');
      if (onFallback) {
        onFallback();
      }
      return;
    }

    if (chartRangeSelector) {
      chartRangeSelector.style.display = 'none';
    }
    if (canvas) {
      canvas.style.display = 'none';
    }

    loadTradingViewScript()
      .then(() => {
        if (!window.TradingView || typeof window.TradingView.widget !== 'function') {
          throw new Error('TradingView 라이브러리가 초기화되지 않았습니다.');
        }

        try {
          new window.TradingView.widget({
            container_id: 'tradingviewWidget',
            autosize: true,
            symbol,
            interval: '60',
            timezone: 'Etc/UTC',
            theme: detectTheme(),
            style: '1',
            locale: 'ko',
            enable_publishing: false,
            hide_side_toolbar: false,
            hide_top_toolbar: false,
            allow_symbol_change: false,
            withdateranges: true,
            details: false,
            hotlist: false,
            calendar: false,
          });
        } catch (error) {
          console.warn('TradingView 위젯 초기화에 실패했습니다. Chart.js 차트로 대체합니다.', error);
          fallbackToChart();
        }
      })
      .catch((error) => {
        console.warn('TradingView 스크립트 로드에 실패했습니다. Chart.js 차트로 대체합니다.', error);
        fallbackToChart();
      });

    function fallbackToChart() {
      if (chartRangeSelector) {
        chartRangeSelector.style.display = '';
      }
      if (canvas) {
        canvas.style.display = '';
      }
      if (onFallback) {
        onFallback();
      }
    }
  }

  function loadTradingViewScript() {
    if (window.TradingView && typeof window.TradingView.widget === 'function') {
      return Promise.resolve();
    }

    if (tradingViewLoader) {
      return tradingViewLoader;
    }

    tradingViewLoader = new Promise((resolve, reject) => {
      const existingScript = document.querySelector('script[src="https://s3.tradingview.com/tv.js"]');
      if (existingScript) {
        existingScript.addEventListener('load', () => resolve(), { once: true });
        existingScript.addEventListener('error', () => reject(new Error('TradingView 스크립트 로드 실패')), { once: true });
        return;
      }

      const scriptEl = document.createElement('script');
      scriptEl.src = 'https://s3.tradingview.com/tv.js';
      scriptEl.async = true;
      scriptEl.onload = () => resolve();
      scriptEl.onerror = () => reject(new Error('TradingView 스크립트 로드 실패'));
      document.head.appendChild(scriptEl);
    });

    return tradingViewLoader;
  }

  function detectTheme() {
    const root = document.documentElement;
    if (root.dataset.theme) {
      return root.dataset.theme === 'dark' ? 'dark' : 'light';
    }
    if (root.classList.contains('dark') || root.classList.contains('theme-dark')) {
      return 'dark';
    }
    return 'light';
  }

  function initializeChartJs(chartJsonInput, coinIdInput, vsCurrencyInput, defaultDaysInput) {
    if (!canvas || !coinIdInput) {
      return;
    }

    let parsed;
    if (chartJsonInput) {
      try {
        parsed = JSON.parse(chartJsonInput);
      } catch (error) {
        console.warn('차트 데이터 파싱에 실패했습니다.', error);
      }
    }

    if (chartRangeSelector) {
      chartRangeSelector.style.display = '';
    }
    canvas.style.display = '';

    const currency = (vsCurrencyInput || 'USD').toLowerCase();
    const buttons = document.querySelectorAll('.chart-range-selector button');
    let currentDays = Number.parseInt(defaultDaysInput || '30', 10);
    let chartInstance = null;

    function formatLabels(points) {
      return points.map((point) => {
        const date = new Date(point.timestamp);
        if (currentDays <= 7) {
          return date.toLocaleString('ko-KR', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
          });
        }
        return date.toLocaleDateString('ko-KR', {
          month: 'short',
          day: 'numeric',
        });
      });
    }

    function extractValues(points) {
      return points.map((point) => point.value);
    }

    function buildChart(points) {
      const labels = formatLabels(points);
      const values = extractValues(points);

      if (chartInstance) {
        chartInstance.data.labels = labels;
        chartInstance.data.datasets[0].data = values;
        chartInstance.options.scales.x.ticks.maxTicksLimit = currentDays <= 7 ? 8 : 12;
        chartInstance.update();
        return;
      }

      chartInstance = new Chart(canvas, {
        type: 'line',
        data: {
          labels,
          datasets: [
            {
              label: `가격 (${vsCurrencyInput || 'USD'})`,
              data: values,
              borderColor: '#38bdf8',
              backgroundColor: 'rgba(56, 189, 248, 0.15)',
              pointRadius: 0,
              borderWidth: 2,
              tension: 0.25,
              fill: true,
            },
          ],
        },
        options: {
          maintainAspectRatio: false,
          responsive: true,
          plugins: {
            legend: {
              display: false,
            },
            tooltip: {
              callbacks: {
                label(context) {
                  const value = context.parsed.y;
                  if (Number.isNaN(value)) {
                    return '';
                  }
                  return `${Number.parseFloat(value).toLocaleString('ko-KR', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })} ${vsCurrencyInput || 'USD'}`;
                },
              },
            },
          },
          scales: {
            y: {
              ticks: {
                color: '#94a3b8',
                callback(value) {
                  if (value >= 1_000_000_000) {
                    return `${(value / 1_000_000_000).toFixed(1)}B`;
                  }
                  if (value >= 1_000_000) {
                    return `${(value / 1_000_000).toFixed(1)}M`;
                  }
                  if (value >= 1_000) {
                    return `${(value / 1_000).toFixed(1)}K`;
                  }
                  return value;
                },
              },
              grid: {
                color: 'rgba(148, 163, 184, 0.1)',
              },
            },
            x: {
              ticks: {
                color: '#94a3b8',
                maxRotation: 0,
                autoSkip: true,
                maxTicksLimit: currentDays <= 7 ? 8 : 12,
              },
              grid: {
                display: false,
              },
            },
          },
        },
      });
    }

    async function fetchChart(days) {
      try {
        const response = await fetch(`/api/coins/${coinIdInput}/market-chart?days=${days}&vs_currency=${currency}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch chart data (${response.status})`);
        }
        const result = await response.json();
        return result.prices || [];
      } catch (error) {
        console.warn('차트 데이터를 불러오지 못했습니다.', error);
        return [];
      }
    }

    function setActiveButton(targetDays) {
      buttons.forEach((button) => {
        button.classList.toggle('is-active', Number.parseInt(button.dataset.days, 10) === targetDays);
      });
    }

    buttons.forEach((button) => {
      button.addEventListener('click', async () => {
        const targetDays = Number.parseInt(button.dataset.days, 10);
        if (Number.isNaN(targetDays) || targetDays === currentDays) {
          return;
        }
        currentDays = targetDays;
        setActiveButton(targetDays);
        const points = await fetchChart(targetDays);
        buildChart(points);
      });
    });

    setActiveButton(currentDays);

    const initialPoints = parsed && Array.isArray(parsed.prices) ? parsed.prices : [];
    if (initialPoints.length > 0) {
      buildChart(initialPoints);
    } else {
      fetchChart(currentDays).then((points) => buildChart(points));
    }
  }
})();
