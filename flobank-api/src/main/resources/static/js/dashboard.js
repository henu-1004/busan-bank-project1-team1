// dashboard.js

document.addEventListener('DOMContentLoaded', function () {
    if (!window.dashboardData) {
        console.warn('dashboardData가 없습니다.');
        return;
    }

    const dataObj = window.dashboardData;

    // 1) 원화 요약 카드 숫자 세팅

    const totalTxCount = dataObj.todayTotalTxCount || 0;      // 오늘 총 거래 건수
    const totalTxAmount = dataObj.todayTotalTxAmount || 0;    // 오늘 총 거래 금액 (원)

    // 카드 1: 총 거래 건수
    const totalTxCountTextEl = document.getElementById('totalTxCountText');
    if (totalTxCountTextEl) {
        totalTxCountTextEl.textContent = totalTxCount.toLocaleString('ko-KR') + '건';
    }

    // 카드 2: 총 거래 금액
    const totalTxAmountTextEl = document.getElementById('totalTxAmountText');
    if (totalTxAmountTextEl) {
        let displayText;
        const oneHundredMillion = 100000000;

        if (totalTxAmount >= oneHundredMillion) {
            const amountInEok = totalTxAmount / oneHundredMillion;
            displayText = amountInEok.toFixed(1) + '억';
        } else {
            displayText = totalTxAmount.toLocaleString('ko-KR') + '원';
        }
        totalTxAmountTextEl.textContent = displayText;
    }


    // 1-1) 전일 대비 % 계산

    const last7 = dataObj.last7Days || [];
    let countDiffPercent = null;
    let amountDiffPercent = null;

    if (Array.isArray(last7) && last7.length >= 2) {
        const todayDaily = last7[last7.length - 1];   // 오늘
        const yesterDaily = last7[last7.length - 2];  // 어제

        const todayCountDaily = todayDaily && todayDaily.count != null ? todayDaily.count : null;
        const yesterdayCountDaily = yesterDaily && yesterDaily.count != null ? yesterDaily.count : null;

        if (yesterdayCountDaily && yesterdayCountDaily > 0 && todayCountDaily != null) {
            countDiffPercent = ((todayCountDaily - yesterdayCountDaily) * 100) / yesterdayCountDaily;
        }

        const todayAmountDaily = todayDaily && todayDaily.amount != null ? todayDaily.amount : null;
        const yesterdayAmountDaily = yesterDaily && yesterDaily.amount != null ? yesterDaily.amount : null;

        if (yesterdayAmountDaily && yesterdayAmountDaily > 0 && todayAmountDaily != null) {
            amountDiffPercent = ((todayAmountDaily - yesterdayAmountDaily) * 100) / yesterdayAmountDaily;
        }
    }

    const countDiffEl = document.getElementById('totalTxCountDiff');
    if (countDiffEl) {
        if (countDiffPercent != null) {
            const rounded = Math.round(countDiffPercent * 10) / 10; // 소수 1자리
            const sign = rounded > 0 ? '+' : '';
            countDiffEl.textContent = `${sign}${rounded}%`;
            countDiffEl.classList.toggle('diff-up', rounded >= 0);
            countDiffEl.classList.toggle('diff-down', rounded < 0);
        } else {
            countDiffEl.textContent = '-';
            countDiffEl.classList.remove('diff-up', 'diff-down');
        }
    }

    const amountDiffEl = document.getElementById('totalTxAmountDiff');
    if (amountDiffEl) {
        if (amountDiffPercent != null) {
            const rounded = Math.round(amountDiffPercent * 10) / 10;
            const sign = rounded > 0 ? '+' : '';
            amountDiffEl.textContent = `${sign}${rounded}%`;
            amountDiffEl.classList.toggle('diff-up', rounded >= 0);
            amountDiffEl.classList.toggle('diff-down', rounded < 0);
        } else {
            amountDiffEl.textContent = '-';
            amountDiffEl.classList.remove('diff-up', 'diff-down');
        }
    }


    // 2) 원화 미니차트 (건수/금액 스파크라인)
    if (typeof Chart === 'undefined') {
        console.warn('Chart.js가 로드되지 않았습니다.');
        return;
    }

    let countTrendData = [];
    let amountTrendData = [];
    let trendLabels = ['6일 전', '5일 전', '4일 전', '3일 전', '2일 전', '1일 전', '오늘'];

    if (Array.isArray(last7) && last7.length > 0) {
        trendLabels = last7.map(d => d.date || '');
        countTrendData = last7.map(d => d.count != null ? d.count : 0);
        amountTrendData = last7.map(d => d.amount != null ? d.amount : 0);
    } else {
        trendLabels = ['오늘'];
        countTrendData = [totalTxCount];
        amountTrendData = [totalTxAmount];
    }

    // (1) 건수 미니차트
    const krwCountCanvas = document.getElementById('krwCountMiniChart');
    if (krwCountCanvas) {
        const ctx = krwCountCanvas.getContext('2d');
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: trendLabels,
                datasets: [
                    {
                        data: countTrendData,
                        borderColor: '#202B44',
                        backgroundColor: 'rgba(32, 43, 68, 0.15)',
                        tension: 0.4,
                        fill: true,
                        pointRadius: 0,
                        hoverRadius: 0,
                        pointHitRadius: 15
                    }
                ]
            },
            options: {
                responsive: true,
                animation: true,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: true,
                        callbacks: {
                            label: function (ctx) {
                                const value = ctx.parsed.y || 0;
                                return value.toLocaleString('ko-KR') + ' 건';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        display: false
                    },
                    y: {
                        display: false,
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // (2) 금액 미니차트
    const krwAmountCanvas = document.getElementById('krwAmountMiniChart');
    if (krwAmountCanvas) {
        const ctx = krwAmountCanvas.getContext('2d');
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: trendLabels,
                datasets: [
                    {
                        data: amountTrendData,
                        borderColor: '#202b44',
                        backgroundColor: 'rgb(92,128,200)',
                        tension: 0.4,
                        fill: true,
                        pointRadius: 0,
                        hoverRadius: 0,
                        pointHitRadius: 15
                    }
                ]
            },
            options: {
                responsive: true,
                animation: true,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        enabled: true,
                        callbacks: {
                            label: function (ctx) {
                                const value = ctx.parsed.y || 0;
                                return value.toLocaleString('ko-KR') + ' 원';
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        display: false
                    },
                    y: {
                        display: false,
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // -----------------------------
    // 3) 외화 거래 수 차트 (환전 / 외화송금)
    // -----------------------------
    const fxList = dataObj.todayFxTxCounts || [];
    const fxCanvas = document.getElementById('fxChart');

    if (fxCanvas && fxList.length > 0) {
        const labels = fxList.map(tx => tx.type);           // "환전", "외화송금"
        const counts = fxList.map(tx => tx.count || 0);
        const maxValue = counts.length > 0 ? Math.max(...counts) : 0;

        const fxCtx = fxCanvas.getContext('2d');
        new Chart(fxCtx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: '오늘 외화 거래 건수',
                        data: counts,
                        backgroundColor: 'rgba(32, 43, 68, 0.7)',
                        barPercentage: 0.2,
                        categoryPercentage: 0.6,
                    }
                ]
            },
            options: {
                responsive: true,
                animation: true,
                plugins: {
                    legend: {
                        display: true,
                        position: 'top'
                    },
                    title: {
                        display: true,
                        text: '오늘 환전·외화송금 거래 건수'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        suggestedMax: maxValue + 1,
                        ticks: {
                            precision: 0
                        }
                    }
                }
            }
        });
    } else if (!fxList.length) {
        console.warn('todayFxTxCounts 데이터가 비어 있습니다.');
    }

    (function () {
        const dataObj = window.dashboardData || {};

        // 일별
        const dailyJoinChartEl = document.getElementById('joinDailyChart');
        if (dailyJoinChartEl) {
            const array = Array.isArray(dataObj.dailyJoinStats) ? dataObj.dailyJoinStats : [];

            const labels = array.map(item =>
                item && item.baseDate ? item.baseDate : ''
            );
            const data = array.map(item =>
                item && typeof item.joinCount === 'number' ? item.joinCount : 0
            );

            // 🔹 최대값 계산 (축 최대값용)
            const maxVal = data.length > 0 ? Math.max(...data) : 0;

            window.joindailyChartInstance = new Chart(dailyJoinChartEl.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: '일별 가입자 수',
                        data: data,
                        borderColor: '#4CAF50',
                        backgroundColor: 'rgba(76, 175, 80, 0.2)',
                        fill: true,
                        tension: 0.4,
                        pointRadius: 3
                    }]
                },
                options: {
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                stepSize: 1,
                                precision: 0 // 정수로만
                            },
                            suggestedMax: maxVal === 0 ? 1 : maxVal + 1
                        }
                    },
                    plugins: {
                        legend: {
                            display: true
                        }
                    },
                    responsive: true,
                    maintainAspectRatio: false
                }
            });
        }
        const weeklyJoinChartEl = document.getElementById('joinWeeklyChart');
        if (weeklyJoinChartEl) {
            // 🔴 오타 수정: weelyJoinStats → weeklyJoinStats
            const weeklyArray = Array.isArray(dataObj.weeklyJoinStats) ? dataObj.weeklyJoinStats : [];

            const labels = weeklyArray.map(item =>
                item && item.baseDate ? item.baseDate : ''
            );
            const data = weeklyArray.map(item =>
                item && typeof item.joinCount === 'number' ? item.joinCount : 0
            );

            const maxVal = data.length > 0 ? Math.max(...data) : 0;

            window.joinweeklyChartInstance = new Chart(weeklyJoinChartEl.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: '주별 가입자 수',
                        data: data,
                        borderColor: '#2196F3',
                        backgroundColor: 'rgba(33, 150, 243, 0.2)',
                        fill: true,
                        tension: 0.4,
                        pointRadius: 3
                    }]
                },
                options: {
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                stepSize: 1,
                                precision: 0
                            },
                            suggestedMax: maxVal === 0 ? 1 : maxVal + 1
                        }
                    },
                    plugins: {
                        legend: {
                            display: true
                        }
                    },
                    responsive: true,
                    maintainAspectRatio: false
                }
            });
        }
        const monthlyJoinChartEl = document.getElementById('joinMonthlyChart');
        if (monthlyJoinChartEl) {
            const monthlyArray = Array.isArray(dataObj.monthlyJoinStats) ? dataObj.monthlyJoinStats : [];

            const labels = monthlyArray.map(item =>
                item && item.baseDate ? item.baseDate : ''
            );
            const data = monthlyArray.map(item =>
                item && typeof item.joinCount === 'number' ? item.joinCount : 0
            );

            const maxVal = data.length > 0 ? Math.max(...data) : 0;

            window.joinmonthlyChartInstance = new Chart(monthlyJoinChartEl.getContext('2d'), {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: '월별 가입자 수',
                        data: data,
                        borderColor: '#FF9800',
                        backgroundColor: 'rgba(255, 152, 0, 0.2)',
                        fill: true,
                        tension: 0.4,
                        pointRadius: 3
                    }]
                },
                options: {
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                stepSize: 1,
                                precision: 0
                            },
                            suggestedMax: maxVal === 0 ? 1 : maxVal + 1
                        }
                    },
                    plugins: {
                        legend: {
                            display: true
                        }
                    },
                    responsive: true,
                    maintainAspectRatio: false
                }
            });
        }

        // dataObj.weeklyJoinStats / monthlyJoinStats 사용해서 생성하면 끝
    })();


});
