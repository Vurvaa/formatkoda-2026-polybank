import { Button, Result } from 'antd';
import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <Result
      status="404"
      title="Страница не найдена"
      subTitle="Проверьте адрес или вернитесь к списку счетов."
      extra={
        <Link to="/accounts">
          <Button type="primary">К счетам</Button>
        </Link>
      }
    />
  );
}
