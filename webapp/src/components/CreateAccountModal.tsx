import { App as AntdApp, Form, Modal, Select } from 'antd';
import { useEffect, useState } from 'react';
import type { AccountType, CreateAccountRequestDto } from '../models/account.ts';
import { accountService } from '../services/accountService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';

interface CreateAccountModalProps {
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

const accountTypeOptions: { label: string; value: AccountType }[] = [
  { label: 'Текущий', value: 'CURRENT' },
  { label: 'Сберегательный', value: 'SAVINGS' },
  { label: 'Вклад', value: 'FIXED_DEPOSIT' },
  { label: 'Кредитный', value: 'CREDIT' }
];

export function CreateAccountModal({ open, onCancel, onSuccess }: Readonly<CreateAccountModalProps>) {
  const [form] = Form.useForm<CreateAccountRequestDto>();
  const [submitting, setSubmitting] = useState(false);
  const { message } = AntdApp.useApp();

  useEffect(() => {
    if (!open) {
      form.resetFields();
    }
  }, [form, open]);

  async function handleSubmit(values: CreateAccountRequestDto) {
    setSubmitting(true);

    try {
      await accountService.create(values);
      message.success('Счет открыт');
      onSuccess();
      onCancel();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось открыть счет'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      title="Открыть новый счет"
      open={open}
      okText="Открыть"
      cancelText="Отмена"
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit} preserve={false}>
        <Form.Item
          name="accountType"
          label="Тип счета"
          rules={[{ required: true, message: 'Выберите тип счета' }]}
        >
          <Select options={accountTypeOptions} placeholder="Выберите тип" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
