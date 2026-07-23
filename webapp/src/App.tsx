import {Navigate, Route, Routes} from 'react-router-dom';
import {AppLayout} from './components/AppLayout.tsx';
import {ManagerRoute} from './components/ManagerRoute.tsx';
import {ProtectedRoute} from './components/ProtectedRoute.tsx';
import {AccountDetailsPage} from './pages/AccountDetailsPage.tsx';
import {AccountsPage} from './pages/AccountsPage.tsx';
import {LoginPage} from './pages/LoginPage.tsx';
import {NotFoundPage} from './pages/NotFoundPage.tsx';
import {ProfilePage} from './pages/ProfilePage.tsx';
import {RegisterPage} from './pages/RegisterPage.tsx';
import {StatisticsPage} from './pages/StatisticsPage.tsx';
import {UserDetailsPage} from './pages/UserDetailsPage.tsx';
import {UsersPage} from './pages/UsersPage.tsx';
import {OAuthCallbackPage} from "./pages/OAuthCallbackPage.tsx";

export function App() {
    return (
        <Routes>
            <Route path="/oauth/callback" element={<OAuthCallbackPage/>}/>
            <Route path="/login" element={<LoginPage/>}/>
            <Route path="/register" element={<RegisterPage/>}/>
            <Route element={<ProtectedRoute/>}>
                <Route element={<AppLayout/>}>
                    <Route path="/" element={<Navigate to="/accounts" replace/>}/>
                    <Route path="/accounts" element={<AccountsPage/>}/>
                    <Route path="/accounts/:accountNumber" element={<AccountDetailsPage/>}/>
                    <Route path="/users" element={<UsersPage/>}/>
                    <Route path="/users/:userLogin" element={<UserDetailsPage/>}/>
                    <Route element={<ManagerRoute/>}>
                        <Route path="/statistics" element={<StatisticsPage/>}/>
                    </Route>
                    <Route path="/profile" element={<ProfilePage/>}/>
                </Route>
            </Route>
            <Route path="*" element={<NotFoundPage/>}/>
        </Routes>
    );
}
