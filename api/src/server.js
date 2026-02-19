import http from 'node:http';
import { URL } from 'node:url';
import { ingestAndRank } from './ingestion.js';
import { fetchLatestAiRepos, fetchTrendingAiRepos } from './github-source.js';
import { discoverSources } from './ai-discovery.js';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const STATE_PATH = resolve(process.cwd(), 'apps/info-push/api/data/state.json');

function ensureStateDir() {
  const d = dirname(STATE_PATH);
  if (!existsSync(d)) mkdirSync(d, { recursive: true });
}

function loadState() {
  try {
    if (!existsSync(STATE_PATH)) return null;
    const raw = readFileSync(STATE_PATH, 'utf8');
    return JSON.parse(raw || '{}');
  } catch {
    return null;
  }
}

function saveState(state) {
  try {
    ensureStateDir();
    writeFileSync(STATE_PATH, JSON.stringify(state, null, 2), 'utf8');
  } catch {}
}

const restored = loadState() || {};

let feed = ingestAndRank('ai');
let sources = Array.isArray(restored.sources)
  ? restored.sources.map((s) => ({ enabled: true, fetchMode: 'hybrid', ...s }))
  : [];
let sourceItems = Array.isArray(restored.sourceItems) ? restored.sourceItems : [];

let messages = [];
let preferences = restored.preferences || {
  topics: ['ai'],
  pushTimes: ['09:00', '20:00'],
  channels: ['in-app', 'push'],
  refreshMinutes: 10
};

function persist() {
  saveState({ sources, sourceItems, preferences });
}

function json(res, status, data) {
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(data));
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', (chunk) => (body += chunk));
    req.on('end', () => {
      try {
        resolve(JSON.parse(body || '{}'));
      } catch (e) {
        reject(e);
      }
    });
  });
}

function nowIso() {
  return new Date().toISOString();
}

function sortedSources() {
  return [...sources]
    .map((s) => ({ enabled: true, fetchMode: 'hybrid', ...s }))
    .sort((a, b) => {
      const bt = b.lastItemAt ? new Date(b.lastItemAt).getTime() : 0;
      const at = a.lastItemAt ? new Date(a.lastItemAt).getTime() : 0;
      return bt - at;
    });
}

function upsertSourceItem(item) {
  const exists = sourceItems.some((x) => x.sourceId === item.sourceId && x.url === item.url);
  if (!exists) sourceItems = [item, ...sourceItems].slice(0, 5000);
  if (!exists) persist();
  return !exists;
}

async function collectSource(source, limit = 20) {
  const normalizedSource = { enabled: true, fetchMode: 'hybrid', ...source };
  if (!normalizedSource.enabled) return { ok: true, added: 0, items: [] };

  let items = [];
  if ((normalizedSource.url || '').includes('github.com') || normalizedSource.type === 'github') {
    const result = await fetchTrendingAiRepos(limit);
    items = (result.items || []).slice(0, limit).map((it) => ({
      id: `itm-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      sourceId: normalizedSource.id,
      title: it.title,
      summary: it.summaryZh || it.summary || '暂无简介',
      url: it.url,
      publishedAt: it.updatedAt || nowIso(),
      createdAt: nowIso()
    }));
  } else {
    items = [
      {
        id: `itm-${Date.now()}-seed`,
        sourceId: normalizedSource.id,
        title: `${normalizedSource.name} 示例条目`,
        summary: '该来源已接入，后续将按混合抓取策略获取真实内容。',
        url: normalizedSource.url,
        publishedAt: nowIso(),
        createdAt: nowIso()
      }
    ];
  }

  let added = 0;
  for (const item of items) {
    if (upsertSourceItem(item)) added += 1;
  }

  const latest = items[0]?.publishedAt || nowIso();
  sources = sources.map((s) =>
    s.id === normalizedSource.id ? { ...s, lastFetchedAt: nowIso(), lastItemAt: latest, updatedAt: nowIso() } : s
  );
  persist();

  return { ok: true, added, items };
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

  if (req.method === 'GET' && url.pathname === '/v1/sources/home') {
    return json(res, 200, { items: sortedSources() });
  }

  if (req.method === 'GET' && url.pathname === '/v1/sources/github/latest') {
    const limit = Number(url.searchParams.get('limit') || '10');
    const result = await fetchLatestAiRepos(limit);
    return json(res, 200, result);
  }

  if (req.method === 'GET' && url.pathname === '/v1/sources/github/trending') {
    const limit = Number(url.searchParams.get('limit') || '10');
    const result = await fetchTrendingAiRepos(limit);
    return json(res, 200, result);
  }

  if (req.method === 'POST' && url.pathname === '/v1/ai/discover-sources') {
    try {
      const payload = await readJsonBody(req);
      const query = String(payload?.query || 'ai');
      const limit = Number(payload?.limit || 8);
      const result = await discoverSources(query, limit);
      return json(res, 200, { ok: true, query, ...result });
    } catch {
      return json(res, 400, { ok: false, error: 'invalid_json' });
    }
  }

  if (req.method === 'GET' && url.pathname === '/v1/sources') {
    return json(res, 200, { items: sortedSources() });
  }

  if (req.method === 'POST' && url.pathname === '/v1/sources') {
    try {
      const payload = await readJsonBody(req);
      const item = {
        id: payload.id || `src-${Date.now()}`,
        type: payload.type || 'custom',
        name: payload.name || 'Untitled Source',
        url: payload.url || '',
        reason: payload.reason || '',
        tags: Array.isArray(payload.tags) ? payload.tags : [],
        fetchMode: payload.fetchMode || 'hybrid',
        enabled: payload.enabled !== false,
        createdAt: nowIso(),
        updatedAt: nowIso(),
        lastFetchedAt: null,
        lastItemAt: null
      };

      if (!item.url) return json(res, 400, { ok: false, error: 'url_required' });
      const exists = sources.some((s) => s.url === item.url);
      if (!exists) sources = [item, ...sources].slice(0, 500);
      persist();

      return json(res, 200, { ok: true, item, duplicated: exists });
    } catch {
      return json(res, 400, { ok: false, error: 'invalid_json' });
    }
  }

  const sourceIdMatch = url.pathname.match(/^\/v1\/sources\/([^/]+)$/);
  if (sourceIdMatch) {
    const sourceId = decodeURIComponent(sourceIdMatch[1]);
    const target = sources.find((s) => s.id === sourceId);
    if (!target) return json(res, 404, { ok: false, error: 'source_not_found' });

    if (req.method === 'PUT') {
      try {
        const payload = await readJsonBody(req);
        sources = sources.map((s) =>
          s.id === sourceId
            ? {
                ...s,
                name: payload.name ?? s.name,
                url: payload.url ?? s.url,
                type: payload.type ?? s.type,
                tags: Array.isArray(payload.tags) ? payload.tags : s.tags,
                fetchMode: payload.fetchMode ?? s.fetchMode,
                enabled: typeof payload.enabled === 'boolean' ? payload.enabled : s.enabled,
                updatedAt: nowIso()
              }
            : s
        );
        persist();
        return json(res, 200, { ok: true });
      } catch {
        return json(res, 400, { ok: false, error: 'invalid_json' });
      }
    }

    if (req.method === 'DELETE') {
      sources = sources.filter((s) => s.id !== sourceId);
      sourceItems = sourceItems.filter((it) => it.sourceId !== sourceId);
      persist();
      return json(res, 200, { ok: true });
    }
  }

  const sourceItemsMatch = url.pathname.match(/^\/v1\/sources\/([^/]+)\/items$/);
  if (sourceItemsMatch && req.method === 'GET') {
    const sourceId = decodeURIComponent(sourceItemsMatch[1]);
    const limitRaw = Number(url.searchParams.get('limit') || '20');
    const limit = Number.isFinite(limitRaw) && limitRaw > 0 ? Math.min(200, Math.floor(limitRaw)) : 20;
    const items = sourceItems
      .filter((it) => it.sourceId === sourceId)
      .sort((a, b) => new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime())
      .slice(0, limit);
    return json(res, 200, { items, limit, sourceId });
  }

  const sourceCollectMatch = url.pathname.match(/^\/v1\/sources\/([^/]+)\/collect$/);
  if (sourceCollectMatch && req.method === 'POST') {
    const sourceId = decodeURIComponent(sourceCollectMatch[1]);
    const source = sources.find((s) => s.id === sourceId);
    if (!source) return json(res, 404, { ok: false, error: 'source_not_found' });
    const limit = Number(url.searchParams.get('limit') || '20');
    const result = await collectSource(source, limit);
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
    try {
      const payload = await readJsonBody(req);
      preferences = { ...preferences, ...payload };
      persist();
      return json(res, 200, { ok: true, preferences });
    } catch {
      return json(res, 400, { ok: false, error: 'invalid_json' });
    }
  }

  if (req.method === 'POST' && url.pathname === '/v1/push/trigger') {
    const item = {
      id: `msg-${Date.now()}`,
      title: 'AI push (manual trigger)',
      body: 'MVP trigger endpoint is active.',
      createdAt: nowIso()
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
