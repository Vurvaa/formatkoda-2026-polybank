import { Alert, Space, Typography } from 'antd';

const GRAFANA_BASE_URL = 'http://192.168.130.81:3000';
const PUBLIC_DASHBOARD_UID = 'ffcbe9435344404c9f4f846c5d6bcd56';

const DASHBOARD_EMBED_URL =
    `${GRAFANA_BASE_URL}/public-dashboards/${PUBLIC_DASHBOARD_UID}` +
    '?theme=light&from=now-24h&to=now&timezone=browser&refresh=1m';

export function StatisticsPage() {
  return (
    <Space direction="vertical" size="large" className="page-stack" style={{ width: '100%' }}>
      <Typography.Title level={2} style={{ margin: 0 }}>
        Статистика
      </Typography.Title>

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