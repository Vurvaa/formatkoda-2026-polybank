const responseElement = document.getElementById("response");

async function request(path, options = {}) {
    try {
        responseElement.textContent = "Загрузка...";

        const response = await fetch(path, options);
        const contentType = response.headers.get("content-type") ?? "";

        let body;

        if (contentType.includes("application/json"))
            body = await response.json();
        else
            body = await response.text();

        responseElement.textContent = JSON.stringify(
            {
                status: response.status,
                ok: response.ok,
                body: body || null
            },
            null,
            2
        );
    } catch (error) {
        responseElement.textContent = `Ошибка: ${error.message}`;
    }
}

function startGenerator() {
    return request("/generator/start", {
        method: "POST"
    });
}

function stopGenerator() {
    return request("/generator/stop", {
        method: "POST"
    });
}

function getStatus() {
    return request("/generator/status");
}

function getMetrics() {
    return request("/generator/metrics");
}

function setConfig(configName) {
    return request(`/generator/config/${configName}`, {
        method: "PUT"
    });
}