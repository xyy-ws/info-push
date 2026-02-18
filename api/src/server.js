import http from 'node:http';
import { URL } from 'node:url';
import { ingestAndRank } from './ingestion.js';
import { fetchLatestAiRepos } from './github-source.js';

let feed = ingestAndRank('ai');

let messages = [];
let preferences = {
  topics: ['ai'],
  pushTimes: ['09:00', '20:00'],
  channels: ['in-app', 'push']
};

function json(res, status, data) {
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || '/', 'http://localhost');

  if (req.method === 'GET' && url.pathname === '/health') {
    return json(res, 200, { ok: true, service: 'info-push-api' });
  }

  if (req.method === 'GET' && url.pathname === '/v1/feed') {
    const topic = url.searchParams.get('topic') || preferences.topics?.[0] || 'ai';
    const limitRaw = Number(url.searchParams.get('limit') || '20');
    const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(100, Math.floor(limitRaw)) : 20;

    if (url.searchParams.get('refresh') === '1') {
      feed = ingestAndRank(topic);
    }

    return json(res, 200, {
      items: feed.slice(0, limit),
      topic,
      limit,
      preferences
    });
  }

  if (req.method === 'GET' && url.pathname === '/v1/sources/github/latest') {
    const limit = Number(url.searchParams.get('limit') || '10');
    const result = await fetchLatestAiRepos(limit);
    return json(res, 200, result);
  }

  if (req.method === 'GET' && url.pathname === '/v1/messages') {
    const limitRaw = Number(url.searchParams.get('limit') || '50');
    const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(200, Math.floor(limitRaw)) : 50;
    const items = messages.slice(0, limit);
    const unreadCount = items.length;
    return json(res, 200, { items, unreadCount, limit });
  }

  if (req.method === 'GET' && url.pathname === '/v1/preferences') {
    return json(res, 200, { preferences });
  }

  if (req.method === 'POST' && url.pathname === '/v1/preferences') {
    let body = '';
    req.on('data', (chunk) => (body += chunk));
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        preferences = { ...preferences, ...payload };
        return json(res, 200, { ok: true, preferences });
      } catch {
        return json(res, 400, { ok: false, error: 'invalid_json' });
      }
    });
    return;
  }

  if (req.method === 'POST' && url.pathname === '/v1/push/trigger') {
    const item = {
      id: `msg-${Date.now()}`,
      title: 'AI push (manual trigger)',
      body: 'MVP trigger endpoint is active.',
      createdAt: new Date().toISOString()
    };
    messages = [item, ...messages].slice(0, 50);
    return json(res, 200, { ok: true, message: item });
  }

  return json(res, 404, { ok: false, error: 'not_found' });
});

const port = Number(process.env.PORT || 8787);
server.listen(port, () => {
  console.log(`[info-push-api] listening on :${port}`);
});
