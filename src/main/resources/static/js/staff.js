/* Staff workspace helpers: confirm prompts, client-side table search, close menu on Escape. */
(function () {
    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        var handler = function (e) {
            if (!window.confirm(el.getAttribute('data-confirm'))) { e.preventDefault(); e.stopImmediatePropagation(); }
        };
        if (el.tagName === 'FORM') el.addEventListener('submit', handler); else el.addEventListener('click', handler);
    });
    // <input data-filter="#tableId"> hides rows that don't contain the typed text
    document.querySelectorAll('[data-filter]').forEach(function (input) {
        var table = document.querySelector(input.getAttribute('data-filter'));
        if (!table) return;
        input.addEventListener('input', function () {
            var q = input.value.trim().toLowerCase();
            table.querySelectorAll('tbody tr').forEach(function (tr) {
                if (tr.hasAttribute('data-empty')) return;
                tr.hidden = q && tr.textContent.toLowerCase().indexOf(q) === -1;
            });
        });
    });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') document.body.classList.remove('side-open'); });
})();

/* Bulk delete: <form data-bulk> with [data-bulk-start], [data-bulk-bar], [data-bulk-delete], [data-bulk-cancel],
   checkbox cells with class "sel-col" and inputs name="ids". */
(function () {
    document.querySelectorAll('form[data-bulk]').forEach(function (form) {
        var start = form.querySelector('[data-bulk-start]'), bar = form.querySelector('[data-bulk-bar]'),
            del = form.querySelector('[data-bulk-delete]'), cancel = form.querySelector('[data-bulk-cancel]');
        var checks = Array.prototype.slice.call(form.querySelectorAll('input[name=ids]'));
        var cols = form.querySelectorAll('.sel-col');
        var on = false;
        function count() {
            var n = checks.filter(function (c) { return c.checked; }).length;
            del.textContent = 'Delete selected (' + n + ')';
            del.disabled = n === 0;
            return n;
        }
        function set(active) {
            on = active;
            cols.forEach(function (c) { c.hidden = !active; });
            checks.forEach(function (c) { c.checked = false; c.disabled = !active; });
            bar.hidden = !active; start.hidden = active;
            count();
        }
        if (!start) return;
        start.addEventListener('click', function () { set(true); if (checks[0]) checks[0].focus(); });
        cancel.addEventListener('click', function () { set(false); start.focus(); });
        checks.forEach(function (c) { c.addEventListener('change', count); });
        form.addEventListener('submit', function (e) {
            var n = count();
            if (!on || n === 0 || !window.confirm('Permanently delete ' + n + ' selected record(s)? This can\'t be undone.')) e.preventDefault();
        });
        set(false);
    });
})();
