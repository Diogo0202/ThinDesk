document.querySelectorAll('form.acao').forEach(form => {
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const button = form.querySelector('button');
        const error = form.querySelector('[role="alert"]');
        error.hidden = true;
        button.disabled = true;
        try {
            const response = await fetch(form.dataset.url, {
                method: form.dataset.method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(Object.fromEntries(new FormData(form)))
            });
            if (response.redirected || response.status === 401) {
                window.location.assign('/login');
                return;
            }
            if (!response.ok) {
                error.textContent = response.headers.get('content-type')?.startsWith('text/plain')
                    ? await response.text() : 'Não foi possível salvar o chamado.';
                error.hidden = false;
                return;
            }
            window.location.reload();
        } catch {
            error.textContent = 'Conexão indisponível. Tente novamente.';
            error.hidden = false;
        } finally {
            button.disabled = false;
        }
    });
});
