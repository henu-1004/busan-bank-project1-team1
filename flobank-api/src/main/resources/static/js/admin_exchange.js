(function () {
    document.addEventListener('DOMContentLoaded', function () {
        if (typeof Chart === 'undefined') {
            console.warn('Chart.js not loaded');
            return;
        }

        const stats = window.exchangeStats || {};
        const currencyData = Array.isArray(stats.currencyDailyAmounts) ? stats.currencyDailyAmounts : [];
        const totalData = Array.isArray(stats.dailyTotals) ? stats.dailyTotals : [];

        const dateLabels = [...new Set(currencyData.map((item) => item.date))].sort();
        const currencies = [...new Set(currencyData.map((item) => item.currency))];

        const palette = ['#5c80c8', '#74b49b', '#f4a261', '#8e83c4', '#ff6b6b', '#2d6a4f', '#4d908e'];

        const stackedDataSets = currencies.map((code, idx) => {
            const values = dateLabels.map((d) => {
                const found = currencyData.find((item) => item.date === d && item.currency === code);
                return found ? Number(found.amount) : 0;
            });

            return {
                label: code,
                data: values,
                backgroundColor: palette[idx % palette.length],
                borderRadius: 6,
                maxBarThickness: 38
            };
        });

        const currencyCtx = document.getElementById('currencyDailyChart');
        if (currencyCtx) {
            new Chart(currencyCtx.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: dateLabels,
                    datasets: stackedDataSets
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: { usePointStyle: true }
                        },
                        tooltip: {
                            callbacks: {
                                label: (ctx) => {
                                    const value = ctx.parsed.y || 0;
                                    return `${ctx.dataset.label}: ${value.toLocaleString('ko-KR')}원`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: { stacked: true },
                        y: {
                            stacked: true,
                            beginAtZero: true,
                            ticks: {
                                callback: (value) => `${Number(value).toLocaleString('ko-KR')}원`
                            }
                        }
                    }
                }
            });
        }

        const totalLabels = totalData.map((item) => item.date);
        const totalValues = totalData.map((item) => Number(item.amount));

        const totalCtx = document.getElementById('dailyTotalChart');
        if (totalCtx) {
            new Chart(totalCtx.getContext('2d'), {
                type: 'bar',
                data: {
                    labels: totalLabels,
                    datasets: [
                        {
                            data: totalValues,
                            backgroundColor: '#202b44',
                            borderRadius: 6,
                            maxBarThickness: 40
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: (ctx) => {
                                    const value = ctx.parsed.y || 0;
                                    return `${value.toLocaleString('ko-KR')}원`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: { stacked: false },
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: (value) => `${Number(value).toLocaleString('ko-KR')}원`
                            }
                        }
                    }
                }
            });
        }
    });
})();