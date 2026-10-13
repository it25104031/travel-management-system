/* Public website behaviour: hero logo fade on scroll, password toggles, live booking totals, payment tabs. */
(function () {
    var body = document.body;
    var reduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Home: the big hero logo fades and lifts as you scroll; the small header logo takes over.
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

    // Show / hide password
    document.querySelectorAll('[data-pw-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var input = document.getElementById(btn.getAttribute('data-pw-toggle'));
            var show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.textContent = show ? 'Hide' : 'Show';
            btn.setAttribute('aria-pressed', show ? 'true' : 'false');
        });
    });

    // Live total on the tour page and in the cart
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

    // Payment method tabs (card / bank transfer)
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

    // Card preview + input formatting (nothing leaves the form until you press Pay)
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

    // Confirm buttons
    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            if (!window.confirm(el.getAttribute('data-confirm'))) e.preventDefault();
        });
    });
})();
