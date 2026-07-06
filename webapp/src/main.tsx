import React from 'react';
import ReactDOM from 'react-dom/client';
import { App as AntdApp, ConfigProvider } from 'antd';
import ruRU from 'antd/locale/ru_RU';
import { BrowserRouter } from 'react-router-dom';
import 'antd/dist/reset.css';
import './index.css';
import { App } from './App.tsx';
import { AuthProvider } from './hooks/useAuth.tsx';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ConfigProvider locale={ruRU}>
      <AntdApp>
        <BrowserRouter
            future={{
                v7_startTransition: true,
                v7_relativeSplatPath: true,
            }}
        >
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </AntdApp>
    </ConfigProvider>
  </React.StrictMode>
);
