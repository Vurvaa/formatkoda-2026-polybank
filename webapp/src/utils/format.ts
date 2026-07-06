import dayjs from 'dayjs';

export function formatMoney(value: number): string {
  const numberValue = Number(value);

  if (Number.isNaN(numberValue)) {
    return `${String(value)} RUB`;
  }

  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(numberValue);
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '-';
  }

  return dayjs(value).format('DD.MM.YYYY HH:mm');
}

