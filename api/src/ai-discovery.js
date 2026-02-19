const DEFAULT_SOURCES = [
  { type: 'github', name: 'GitHub Trending AI', url: 'https://github.com/topics/ai', tags: ['github', 'ai', 'trending'] },
  { type: 'github', name: 'GitHub Topic: llm', url: 'https://github.com/topics/llm', tags: ['github', 'llm'] },
  { type: 'rss', name: 'Hugging Face Blog', url: 'https://huggingface.co/blog/feed.xml', tags: ['ml', 'models'] },
  { type: 'rss', name: 'OpenAI News', url: 'https://openai.com/news/rss.xml', tags: ['openai'] },
  { type: 'rss', name: 'Google AI Blog', url: 'https://blog.google/technology/ai/rss/', tags: ['google', 'ai'] },
  { type: 'social', name: 'Reddit /r/MachineLearning', url: 'https://www.reddit.com/r/MachineLearning/.rss', tags: ['reddit', 'research'] },
  { type: 'social', name: 'YouTube AI Topic', url: 'https://www.youtube.com/results?search_query=AI', tags: ['youtube', 'ai'] }
];

function scoreSource(source, query) {
  const q = String(query || '').toLowerCase();
  const base = `${source.name} ${source.url} ${(source.tags || []).join(' ')}`.toLowerCase();
  let s = 0;
  for (const token of q.split(/\s+/).filter(Boolean)) {
    if (base.includes(token)) s += 20;
  }
  if (base.includes('ai')) s += 5;
  return s;
}

async function relayDiscover(query, limit = 8) {
  const relay = process.env.OPENCLAW_AGENT_RELAY_URL || '';
  if (!relay) return null;

  const res = await fetch(relay, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      task: 'discover_sources',
      query,
      limit,
      format: 'json'
    })
  });

  if (!res.ok) throw new Error(`relay_status_${res.status}`);
  const data = await res.json();
  if (!Array.isArray(data?.items)) return null;

  return data.items.slice(0, limit).map((it, i) => ({
    id: `relay-${Date.now()}-${i}`,
    type: it.type || 'custom',
    name: it.name || `Source ${i + 1}`,
    url: it.url || '',
    reason: it.reason || 'AI 推荐来源',
    tags: it.tags || []
  }));
}

export async function discoverSources(query, limit = 8) {
  const q = String(query || '').trim() || 'ai';

  try {
    const relayItems = await relayDiscover(q, limit);
    if (relayItems?.length) {
      return { mode: 'relay', items: relayItems };
    }
  } catch {
    // fallback below
  }

  const items = DEFAULT_SOURCES
    .map((s) => ({ ...s, score: scoreSource(s, q) }))
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((s, i) => ({
      id: `fallback-${Date.now()}-${i}`,
      type: s.type,
      name: s.name,
      url: s.url,
      reason: `与“${q}”相关度较高`,
      tags: s.tags || []
    }));

  return { mode: 'fallback', items };
}
