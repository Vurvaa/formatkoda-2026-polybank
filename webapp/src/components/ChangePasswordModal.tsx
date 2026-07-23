import { App as AntdApp, Form, Input, Modal } from 'antd';
import { useState } from 'react';
import { userService } from '../services/userService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';

interface ChangePasswordModalProps {
  open: boolean;
  login: string;
  onCancel: () => void;
  onSuccess: () => void;
}

interface ChangePasswordFormValues {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export function ChangePasswordModal({ open, login, onCancel, onSuccess }: Readonly<ChangePasswordModalProps>) {
  const [form] = Form.useForm<ChangePasswordFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const { message } = AntdApp.useApp();

  async function handleSubmit(values: ChangePasswordFormValues) {
    setSubmitting(true);

    try {
      await userService.changePassword({
        login,
        oldPassword: values.oldPassword,
        newPassword: values.newPassword
      });
      message.success('Пароль изменен');
      onSuccess();
      onCancel();
    } catch (error) {
      message.error(getApiErrorMessage(error, 'Не удалось изменить пароль'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Modal
      title="Смена пароля"
      open={open}
      okText="Изменить"
      cancelText="Отмена"
      confirmLoading={submitting}
      onCancel={onCancel}
      onOk={() => form.submit()}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleSubmit} preserve={false}>
        <Form.Item
          name="oldPassword"
          label="Текущий пароль"
          rules={[{ required: true, message: 'Введите текущий пароль' }]}
        >
          <Input.Password autoComplete="current-password" />
        </Form.Item>

        <Form.Item
          name="newPassword"
          label="Новый пароль"
          rules={[
            { required: true, message: 'Введите новый пароль' },
            { min: 8, message: 'Пароль должен содержать минимум 8 символов' }
          ]}
          hasFeedback
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>

        <Form.Item
          name="confirmPassword"
          label="Повторите новый пароль"
          dependencies={['newPassword']}
          hasFeedback
          rules={[
            { required: true, message: 'Повторите новый пароль' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error('Пароли не совпадают'));
              }
            })
          ]}
        >
          <Input.Password autoComplete="new-password" />
        </Form.Item>
      </Form>
    </Modal>
  );
}