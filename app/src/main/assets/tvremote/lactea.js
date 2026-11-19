/* Lactea's web remote: the TV Remote core with Lactea's look, and a "now playing" bar. */
(function () {
  'use strict';
  var app = document.getElementById('app');
  var now = document.getElementById('now');
  var state = null;

  function post(screen, id, value) {
    var body = { client: TVRemote.client, screen: screen, id: id };
    if (value !== undefined) body.value = value;
    return fetch(value === undefined ? 'api/action' : 'api/set', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    }).catch(function () {});
  }

  /** The bar at the bottom: preset playing, previous / favourite / next, a touch opens its details. */
  function showState(s) {
    state = s;
    var p = s.preset;
    now.hidden = !p;
    if (!p) return;
    var img = document.getElementById('now-img');
    if (p.img) { img.src = p.img; img.hidden = false; } else img.hidden = true;
    document.getElementById('now-name').textContent = p.name;
    var sub = p.category;
    if (s.track) sub = s.track.title + (s.track.artist ? ' — ' + s.track.artist : '');
    document.getElementById('now-sub').textContent = sub;
    var fav = document.getElementById('now-fav');
    fav.textContent = s.favorite ? '★' : '☆';
    fav.classList.toggle('on', !!s.favorite);
  }

  now.addEventListener('click', function (ev) {
    var b = ev.target.closest('button');
    if (b) {
      var act = b.dataset.act;
      if (act === 'fav') post('live', 'fav', !(state && state.favorite));
      else post('live', act);
      return;
    }
    if (state && state.preset) TVRemote.go('preset:' + state.preset.key);
  });

  // On a computer: Escape goes back, ← / → change the preset (outside text fields).
  document.addEventListener('keydown', function (ev) {
    if (ev.ctrlKey || ev.metaKey || ev.altKey) return;
    var t = ev.target;
    if (t && (t.tagName === 'INPUT' || t.tagName === 'SELECT' || t.tagName === 'TEXTAREA')) return;
    if (document.querySelector('.tr-dialog')) return;
    if (ev.key === 'Escape') TVRemote.back();
    else if (ev.key === 'ArrowLeft') post('live', 'prev');
    else if (ev.key === 'ArrowRight') post('live', 'next');
    else return;
    ev.preventDefault();
  });

  TVRemote.start(app, {
    state: showState,
    screen: function (el, s) {
      document.body.dataset.screen = s.id.split(':')[0];
      document.title = s.title + ' · Lactea';
    }
  });
})();
