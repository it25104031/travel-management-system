(function () {
    var body = document.body;
    var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    var heroLogo = document.querySelector('[data-hero-logo]');
    if (body.classList.contains('home')) {
        var ticking = false;
        var update = function () {
            var y = window.scrollY || 0;
            var h = Math.max(1, window.innerHeight * 0.55);
            var t = Math.min(1, y / h);
            if (heroLogo && !reduced) {
                heroLogo.style.opacity = String(1 - t);
                heroLogo.style.transform = 'translateY(' + (-40 * t) + 'px) scale(' + (1 - 0.06 * t) + ')';
            }
            body.classList.toggle('scrolled', y > h * 0.8);
            ticking = false;
        };
        window.addEventListener('scroll', function () {
            if (!ticking) { window.requestAnimationFrame(update); ticking = true; }
        }, { passive: true });
        update();
    }

    document.querySelectorAll('[data-pw-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var input = document.getElementById(btn.getAttribute('data-pw-toggle'));
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.textContent = show ? 'Hide' : 'Show';
            btn.setAttribute('aria-pressed', show ? 'true' : 'false');
        });
    });

    var fmt = function (n) { return 'LKR ' + n.toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); };
    document.querySelectorAll('[data-price]').forEach(function (form) {
        var price = parseFloat(form.getAttribute('data-price'));
        var input = form.querySelector('[name=travellers]');
        var out = form.querySelector('[data-total]');
        if (!input || !out) return;
        var calc = function () {
            var n = parseInt(input.value, 10);
            out.textContent = n > 0 ? fmt(price * n) : '-';
        };
        input.addEventListener('input', calc);
        calc();
    });

    var tabs = document.querySelectorAll('[data-pay-tab]');
    tabs.forEach(function (tab) {
        tab.addEventListener('click', function () {
            tabs.forEach(function (t) {
                var on = t === tab;
                t.setAttribute('aria-selected', on ? 'true' : 'false');
                document.getElementById(t.getAttribute('data-pay-tab')).hidden = !on;
            });
        });
    });

    var num = document.getElementById('cardNumber');
    if (num) {
        var prevNum = document.querySelector('[data-card-num]');
        var prevName = document.querySelector('[data-card-name]');
        var prevExp = document.querySelector('[data-card-exp]');
        var name = document.getElementById('cardName');
        var exp = document.getElementById('expiry');
        num.addEventListener('input', function () {
            var d = num.value.replace(/\D/g, '').slice(0, 19);
            num.value = d.replace(/(.{4})/g, '$1 ').trim();
            prevNum.textContent = (d + '•'.repeat(Math.max(0, 16 - d.length))).replace(/(.{4})/g, '$1 ').trim();
        });
        exp.addEventListener('input', function () {
            var d = exp.value.replace(/\D/g, '').slice(0, 4);
            exp.value = d.length > 2 ? d.slice(0, 2) + '/' + d.slice(2) : d;
            prevExp.textContent = exp.value || 'MM/YY';
        });
        name.addEventListener('input', function () { prevName.textContent = name.value || 'Name on card'; });
    }

    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            if (!window.confirm(el.getAttribute('data-confirm'))) e.preventDefault();
        });
    });
})();
