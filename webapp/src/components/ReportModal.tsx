import {App as AntdApp, Form, InputNumber, Modal, Select} from 'antd';
import { useEffect, useState } from 'react';
import { accountService } from '../services/accountService.ts';
import { getApiErrorMessage } from '../utils/errors.ts';
import type {ReportFormat} from "../models/transaction.ts";

interface ReportModalProps {
    accountNumber: string;
    open: boolean;
    onCancel: () => void;
}

interface ReportFormValues {
    transactionsCount: number;
    format: ReportFormat;
}

const countRules = [
    { required: true, message: 'Введите количество транзакций' },
    {
        validator(_: unknown, value: string | number | null) {
            if (!value || Number(value) < 1) {
                return Promise.reject(new Error('Число транзакций не может быть меньше 1'));
            }

            return Promise.resolve();
        }
    }
];

export function ReportModal({accountNumber, open, onCancel}: Readonly<ReportModalProps>) {
    const [form] = Form.useForm<ReportFormValues>();
    const [submitting, setSubmitting] = useState(false);
    const { message } = AntdApp.useApp();

    useEffect(() => {
        if (!open) {
            form.resetFields();
        }
    }, [form, open]);

    async function handleSubmit(values: ReportFormValues) {
        setSubmitting(true);

        try {
            const file = await accountService.getReport(accountNumber, values.transactionsCount, values.format);

            const url = URL.createObjectURL(file);
            const link = document.createElement('a');

            link.href = url;
            link.download = `report_${accountNumber}.${values.format.toLowerCase()}`;

            document.body.appendChild(link);
            link.click();
            link.remove();

            URL.revokeObjectURL(url);

            message.success('Выписка сформирована');
            onCancel();
        } catch (error) {
            message.error(getApiErrorMessage(error, 'Не удалось выполнить операцию'));
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Modal
            title="Выписка по счёту"
            open={open}
            okText="Запросить"
            cancelText="Отмена"
            confirmLoading={submitting}
            onCancel={onCancel}
            onOk={() => form.submit()}
            destroyOnHidden
        >
            <Form form={form} layout="vertical" onFinish={handleSubmit} preserve={false}>
                <Form.Item name="transactionsCount" label="Количество операций" rules={countRules}>
                    <InputNumber
                        min="1"
                        step="1"
                        precision={0}
                        className="full-width"
                        placeholder="20"
                    />
                </Form.Item>
                <Form.Item
                    name="format"
                    label="Формат"
                    rules={[
                        { required: true, message: 'Выберите формат выписки' }
                    ]}
                >
                    <Select<ReportFormat>
                        placeholder="Выберите формат"
                        options={[
                            { value: 'CSV', label: 'CSV' },
                            { value: 'PDF', label: 'PDF' }
                        ]}
                    />
                </Form.Item>
            </Form>
        </Modal>
    );
}
