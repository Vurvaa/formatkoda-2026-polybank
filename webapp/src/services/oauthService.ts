const AUTH_SERVER = 'http://localhost:9091';
const CLIENT_ID = 'frontend';
const REDIRECT_URI = 'http://localhost:5173/oauth/callback';
const SCOPE = 'openid profile api';

const VERIFIER_KEY = 'oauth.code_verifier';
const STATE_KEY = 'oauth.state';
const RETURN_TO_KEY = 'oauth.return_to';

export interface OAuthTokenResponse {
    access_token: string;
    token_type: string;
    expires_in: number;
    scope?: string;
    id_token?: string;
}

function toBase64Url(bytes: Uint8Array): string {
    return btoa(String.fromCharCode(...bytes))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/g, '');
}

function generateRandomValue(): string {
    const bytes = new Uint8Array(32);
    crypto.getRandomValues(bytes);
    return toBase64Url(bytes);
}

async function createCodeChallenge(verifier: string): Promise<string> {
    const bytes = new TextEncoder().encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', bytes);

    return toBase64Url(new Uint8Array(digest));
}

export async function startLogin(returnTo = '/accounts'): Promise<void> {
    const codeVerifier = generateRandomValue();
    const state = generateRandomValue();
    const codeChallenge = await createCodeChallenge(codeVerifier);

    sessionStorage.setItem(VERIFIER_KEY, codeVerifier);
    sessionStorage.setItem(STATE_KEY, state);
    sessionStorage.setItem(RETURN_TO_KEY, returnTo);

    const params = new URLSearchParams({
        response_type: 'code',
        client_id: CLIENT_ID,
        redirect_uri: REDIRECT_URI,
        scope: SCOPE,
        state,
        code_challenge: codeChallenge,
        code_challenge_method: 'S256'
    });

    window.location.assign(
        `${AUTH_SERVER}/oauth2/authorize?${params.toString()}`
    );
}

export async function exchangeCode(
    search: string
): Promise<OAuthTokenResponse> {
    const callbackParams = new URLSearchParams(search);

    const oauthError = callbackParams.get('error');
    if (oauthError) {
        const description = callbackParams.get('error_description');
        throw new Error(description ?? oauthError);
    }

    const code = callbackParams.get('code');
    const returnedState = callbackParams.get('state');
    const expectedState = sessionStorage.getItem(STATE_KEY);
    const codeVerifier = sessionStorage.getItem(VERIFIER_KEY);

    if (!code) {
        throw new Error('Сервер авторизации не вернул authorization code');
    }

    if (!returnedState || returnedState !== expectedState) {
        throw new Error('Некорректный OAuth2 state');
    }

    if (!codeVerifier) {
        throw new Error('Не найден PKCE code verifier');
    }

    const response = await fetch(`${AUTH_SERVER}/oauth2/token`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: CLIENT_ID,
            redirect_uri: REDIRECT_URI,
            code,
            code_verifier: codeVerifier
        })
    });

    const body = await response.json();

    if (!response.ok) {
        throw new Error(
            body.error_description ?? body.error ?? 'Не удалось получить access token'
        );
    }

    sessionStorage.removeItem(VERIFIER_KEY);
    sessionStorage.removeItem(STATE_KEY);

    return body as OAuthTokenResponse;
}

export function getReturnTo(): string {
    return sessionStorage.getItem(RETURN_TO_KEY) ?? '/accounts';
}

export function clearReturnTo(): void {
    sessionStorage.removeItem(RETURN_TO_KEY);
}