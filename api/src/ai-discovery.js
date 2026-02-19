const DEFAULT_SOURCES = [
  { type: 'github', name: 'GitHub Trending AI', url: 'https://github.com/topics/ai', tags: ['github', 'ai', 'trending'] },
  { type: 'github', name: 'GitHub Trending Finance', url: 'https://github.com/topics/finance', tags: ['github', 'finance', 'fintech'] },
  { type: 'github', name: 'GitHub Trending Crypto', url: 'https://github.com/topics/crypto', tags: ['github', 'crypto', 'blockchain'] },
  { type: 'rss', name: 'Hugging Face Blog', url: 'https://huggingface.co/blog/feed.xml', tags: ['ai', 'ml', 'models'] },
  { type: 'rss', name: 'OpenAI News', url: 'https://openai.com/news/rss.xml', tags: ['ai', 'openai'] },
  { type: 'rss', name: 'Google AI Blog', url: 'https://blog.google/technology/ai/rss/', tags: ['ai', 'google'] },
  { type: 'rss', name: 'CoinDesk RSS', url: 'https://www.coindesk.com/arc/outboundfeeds/rss/', tags: ['finance', 'crypto', 'market'] },
  { type: 'rss', name: 'CNBC Top News RSS', url: 'https://www.cnbc.com/id/100003114/device/rss/rss.html', tags: ['finance', 'market', 'business'] },
  { type: 'social', name: 'Reddit /r/MachineLearning', url: 'https://www.reddit.com/r/MachineLearning/.rss', tags: ['ai', 'research', 'reddit'] },
  { type: 'social', name: 'Reddit /r/investing', url: 'https://www.reddit.com/r/investing/.rss', tags: ['finance', 'investing', 'reddit'] }
];

function tokenize(query = '') {
  return String(query)
    .toLowerCase()
    .replace(/[\p{P}\p{S}]+/gu, ' ')
    .split(/\s+/)
    .map((x) => x.trim())
    .filter(Boolean);
}

function expandTokens(tokens = []) {
  const out = new Set(tokens);
  const q = tokens.join(' ');
  if (/财经|金融|投资|股市|market|finance|fintech|stock/.test(q)) {
    ['finance', 'market', 'investing', 'fintech', 'stock', 'macro', 'etf', 'quant', '财经', '金融'].forEach((x) => out.add(x));
  }
  if (/加密|区块链|币|crypto|web3|blockchain/.test(q)) {
    ['crypto', 'blockchain', 'web3', 'bitcoin', 'ethereum', '币圈', '加密'].forEach((x) => out.add(x));
  }
  if (/ai|人工智能|模型|llm|agent/.test(q)) {
    ['ai', 'llm', 'agent', 'machinelearning', 'generativeai', '模型', '人工智能'].forEach((x) => out.add(x));
  }
  return [...out];
}

function scoreSource(source, query) {
  const tokens = expandTokens(tokenize(query));
  const base = `${source.name} ${source.url} ${(source.tags || []).join(' ')}`.toLowerCase();
  let s = 0;
  for (const token of tokens) {
    if (base.includes(String(token).toLowerCase())) s += 30;
  }
  return s;
}

function inferGithubTopic(query = '') {
  const q = String(query || '').toLowerCase();
  if (/财经|金融|finance|fintech|stock|market|投资/.test(q)) return 'finance';
  if (/crypto|币|区块链|web3/.test(q)) return 'crypto';
  if (/医疗|health|bio/.test(q)) return 'health';
  if (/教育|learn|study|edu/.test(q)) return 'education';
  if (/agent/.test(q)) return 'agent';
  if (/llm/.test(q)) return 'llm';
  return 'ai';
}

function injectDynamicSources(query) {
  const topic = inferGithubTopic(query);
  const q = String(query || '').trim();
  return [
    {
      type: 'github',
      name: `GitHub Trending: ${topic}`,
      url: `https://github.com/topics/${topic}`,
      tags: ['github', topic, 'trending']
    },
    {
      type: 'github',
      name: `GitHub Search: ${q}`,
      url: `https://github.com/search?q=${encodeURIComponent(q)}`,
      tags: ['github', ...tokenize(q)]
    },
    {
      type: 'rss',
      name: `Google News RSS: ${q}`,
      url: `https://news.google.com/rss/search?q=${encodeURIComponent(q)}`,
      tags: ['news', ...tokenize(q)]
    }
  ];
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
    reason: it.reason || `根据“${query}”推荐`,
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

  const pool = [...injectDynamicSources(q), ...DEFAULT_SOURCES];
  const ranked = pool
    .map((s) => ({ ...s, score: scoreSource(s, q) }))
    .filter((s) => s.score > 0 || s.url.toLowerCase().includes(inferGithubTopic(q)))
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

  return { mode: 'fallback', items: ranked };
}
