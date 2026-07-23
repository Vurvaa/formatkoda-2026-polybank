import { App, Spin } from 'antd';
import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.tsx';
import {
    clearReturnTo,
    getReturnTo
} from '../services/oauthService.ts';

export function OAuthCallbackPage() {
    const { completeLogin } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    const { message } = App.useApp();
    const started = useRef(false);

    useEffect(() => {
        if (started.current) {
            return;
        }

        started.current = true;

        completeLogin(location.search)
            .then(() => {
                const returnTo = getReturnTo();
                clearReturnTo();
                navigate(returnTo, { replace: true });
            })
            .catch((error: unknown) => {
                const errorMessage =
                    error instanceof Error ? error.message : 'Не удалось войти';

                message.error(errorMessage);
                navigate('/login', { replace: true });
            });
    }, [completeLogin, location.search, message, navigate]);

    return <Spin fullscreen tip="Выполняется вход..." />;
}