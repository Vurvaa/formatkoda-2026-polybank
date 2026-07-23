import { App as AntdApp, Form, Input, InputNumber, Modal } from 'antd';
import { useMemo, useState } from 'react';
import { accountService } from '../services/accountService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';

export type AccountOperation = 'topUp' | 'withdraw' | 'transfer';

interface OperationModalProps {
  accountNumber: string;
  operation: AccountOperation | null;
  open: boolean;
  onCancel: () => void;
  onSuccess: () => void;
}

interface OperationFormValues {
  amount: string;
  toAccountNumber?: string;
}

const operationTitle: Record<AccountOperation, string> = {
  topUp: 'Пополнение счета',
  withdraw: 'Снятие денег',
  transfer: 'Перевод другому пользователю'
};

const operationSubmitText: Record<AccountOperation, string> = {
  topUp: 'Пополнить',
  withdraw: 'Снять',
  transfer: 'Перевести'
};

const accountNumberRules = [
  { required: true, message: 'Введите номер счета' },
  { len: 20, message: 'Номер счета должен состоять из 20 символов' }
];

const amountRules = [
  { required: true, message: 'Введите сумму' },
  {
    validator(_: unknown, value: string | number | null) {
      if (!value || Number(value) < 0.01) {
        return Promise.reject(new Error('Сумма должна быть не меньше 0.01'));
      }

      return Promise.resolve();
    }
  }
];

export function OperationModal({accountNumber, operation, open, onCancel, onSuccess}: Readonly<OperationModalProps>) {
  const [form] = Form.useForm<OperationFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const { message } = AntdApp.useApp();

  const title = useMemo(() => (operation ? operationTitle[operation] : ''), [operation]);
  const submitText = useMemo(
    () => (operation ? operationSubmitText[operation] : 'Выполнить'),
    [operation]
  );

  async function handleSubmit(values: OperationFormValues) {
    if (!operation) {
      return;
    }

    setSubmitting(true);

    try {
      switch (operation) {
        case "topUp": {
          await accountService.topUp({ accountNumber: accountNumber, amount: values.amount });
          break;
        }
        case "withdraw": {
          await accountService.withdraw({ accountNumber: accountNumber, amount: values.amount });
          break;
        }
        case "transfer": {
          await accountService.transfer({
            fromAccountNumber: accountNumber,
            toAccountNumber: values.toAccountNumber ?? '',
            amount: values.amount
          });
        }
      }

      message.success('Операция выполнена');
      onSuccess();
      onCancel();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось выполнить операцию'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      title={title}
      open={open}
      okText={submitText}
      cancelText="Отмена"
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit} preserve={false}>
        {operation === 'transfer' && (
          <Form.Item name="toAccountNumber" label="Номер счета получателя" rules={accountNumberRules}>
            <Input maxLength={20} placeholder="Введите номер счета" />
          </Form.Item>
        )}

        <Form.Item name="amount" label="Сумма" rules={amountRules}>
          <InputNumber
            min="0.01"
            step="0.01"
            stringMode
            precision={2}
            className="full-width"
            placeholder="0.00"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
