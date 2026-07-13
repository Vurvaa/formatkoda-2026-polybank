import { Alert, Space, Typography } from 'antd';

const GRAFANA_BASE_URL = 'http://192.168.130.81:3000';
const DASHBOARD_UID = 'bank-events-overview21';
const DASHBOARD_SLUG = 'bank-analytics';

const DASHBOARD_EMBED_URL =
  `${GRAFANA_BASE_URL}/d/${DASHBOARD_UID}/${DASHBOARD_SLUG}` +
  '?orgId=1&kiosk=tv&theme=light&refresh=5s';

export function StatisticsPage() {
  return (
    <Space direction="vertical" size="large" className="page-stack" style={{ width: '100%' }}>
      <Typography.Title level={2} style={{ margin: 0 }}>
        Статистика
      </Typography.Title>

      <Alert
        type="info"
        showIcon
        message="Дашборд Grafana"
        description="Данные о событиях банка в реальном времени. Если дашборд не загрузился, проверьте доступность Grafana."
      />

      <div
        style={{
          width: '100%',
          height: 'calc(100vh - 260px)',
          minHeight: 600,
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          overflow: 'hidden'
        }}
      >
        <iframe
          title="Bank events overview"
          src={DASHBOARD_EMBED_URL}
          width="100%"
          height="100%"
          style={{ border: 'none' }}
        />
      </div>
    </Space>
  );
}
