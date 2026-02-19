const DEFAULT_EN_SOURCES = [
  { type: 'github', name: 'GitHub Trending AI', url: 'https://github.com/topics/ai', tags: ['github', 'ai', 'trending'], lang: 'en' },
  { type: 'github', name: 'GitHub Trending Finance', url: 'https://github.com/topics/finance', tags: ['github', 'finance', 'fintech'], lang: 'en' },
  { type: 'github', name: 'GitHub Trending Crypto', url: 'https://github.com/topics/crypto', tags: ['github', 'crypto', 'blockchain'], lang: 'en' },
  { type: 'github', name: 'GitHub Trending Quant', url: 'https://github.com/topics/quant', tags: ['github', 'quant', 'finance'], lang: 'en' },

  { type: 'rss', name: 'Hugging Face Blog', url: 'https://huggingface.co/blog/feed.xml', tags: ['ai', 'ml', 'models'], lang: 'en' },
  { type: 'rss', name: 'OpenAI News', url: 'https://openai.com/news/rss.xml', tags: ['ai', 'openai'], lang: 'en' },
  { type: 'rss', name: 'Google AI Blog', url: 'https://blog.google/technology/ai/rss/', tags: ['ai', 'google'], lang: 'en' },

  { type: 'rss', name: 'CoinDesk RSS', url: 'https://www.coindesk.com/arc/outboundfeeds/rss/', tags: ['finance', 'crypto', 'market'], lang: 'en' },
  { type: 'rss', name: 'Cointelegraph RSS', url: 'https://cointelegraph.com/rss', tags: ['finance', 'crypto', 'market'], lang: 'en' },
  { type: 'rss', name: 'Reuters Business RSS', url: 'https://feeds.reuters.com/reuters/businessNews', tags: ['finance', 'business', 'market'], lang: 'en' },
  { type: 'rss', name: 'Reuters World News RSS', url: 'https://feeds.reuters.com/Reuters/worldNews', tags: ['macro', 'market', 'finance'], lang: 'en' },
  { type: 'rss', name: 'CNBC Top News RSS', url: 'https://www.cnbc.com/id/100003114/device/rss/rss.html', tags: ['finance', 'market', 'business'], lang: 'en' },
  { type: 'rss', name: 'MarketWatch Top Stories', url: 'https://feeds.content.dowjones.io/public/rss/mw_topstories', tags: ['finance', 'market', 'stock'], lang: 'en' },
  { type: 'rss', name: 'Yahoo Finance', url: 'https://finance.yahoo.com/news/rssindex', tags: ['finance', 'market', 'stock'], lang: 'en' },

  { type: 'social', name: 'Reddit /r/MachineLearning', url: 'https://www.reddit.com/r/MachineLearning/.rss', tags: ['ai', 'research', 'reddit'], lang: 'en' },
  { type: 'social', name: 'Reddit /r/investing', url: 'https://www.reddit.com/r/investing/.rss', tags: ['finance', 'investing', 'reddit'], lang: 'en' },
  { type: 'social', name: 'Reddit /r/stocks', url: 'https://www.reddit.com/r/stocks/.rss', tags: ['finance', 'stocks', 'reddit'], lang: 'en' },
  { type: 'social', name: 'Reddit /r/SecurityAnalysis', url: 'https://www.reddit.com/r/SecurityAnalysis/.rss', tags: ['finance', 'analysis', 'reddit'], lang: 'en' }
];

const DEFAULT_ZH_SOURCES = [
  { type: 'rss', name: '机器之心', url: 'https://www.jiqizhixin.com/rss', tags: ['中文', 'ai', '人工智能', '科技'], lang: 'zh' },
  { type: 'rss', name: '量子位', url: 'https://www.qbitai.com/feed', tags: ['中文', 'ai', '人工智能', '科技'], lang: 'zh' },
  { type: 'rss', name: '少数派', url: 'https://sspai.com/feed', tags: ['中文', '效率', '科技', '产品'], lang: 'zh' },
  { type: 'rss', name: '36Kr 科技', url: 'https://36kr.com/feed', tags: ['中文', '科技', '创业', '商业'], lang: 'zh' },
  { type: 'rss', name: '开源中国资讯', url: 'https://www.oschina.net/news/rss', tags: ['中文', '开源', '技术', '编程'], lang: 'zh' },
  { type: 'rss', name: 'V2EX 热门', url: 'https://www.v2ex.com/index.xml', tags: ['中文', '社区', '开发', '技术'], lang: 'zh' },
  { type: 'rss', name: '掘金热门', url: 'https://juejin.cn/rss', tags: ['中文', '开发', '前端', '后端'], lang: 'zh' },
  { type: 'rss', name: '知乎热榜（RSSHub）', url: 'https://rsshub.app/zhihu/hotlist', tags: ['中文', '知乎', '热榜', '资讯'], lang: 'zh' }
];

function tokenize(query = '') {
  return String(query)
    .toLowerCase()
    .replace(/[\p{P}\p{S}]+/gu, ' ')
    .split(/\s+/)
    .map((x) => x.trim())
    .filter(Boolean);
}

function hasChinese(query = '') {
  return /[\u3400-\u9fff]/.test(String(query));
}

function isValidHttpUrl(url = '') {
  try {
    const u = new URL(url);
    return u.protocol === 'http:' || u.protocol === 'https:';
  } catch {
    return false;
  }
}

function normalizeUrl(url = '') {
  try {
    const u = new URL(url);
    u.hash = '';
    if (u.pathname.endsWith('/')) u.pathname = u.pathname.slice(0, -1);
    return u.toString();
  } catch {
    return String(url || '').trim();
  }
}

function dedupeAndValidate(sources = []) {
  const seen = new Set();
  const out = [];
  for (const source of sources) {
    if (!isValidHttpUrl(source?.url)) continue;
    const key = normalizeUrl(source.url).toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(source);
  }
  return out;
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
    ['ai', 'llm', 'agent', 'machinelearning', 'generativeai', '模型', '人工智能', '科技'].forEach((x) => out.add(x));
  }
  if (/编程|开发|技术|程序员|开源|coding|programming|developer/.test(q)) {
    ['开发', '编程', '技术', '开源', 'programming', 'developer'].forEach((x) => out.add(x));
  }
  return [...out];
}

function getMatchStats(source, query) {
  const base = `${source.name} ${source.url} ${(source.tags || []).join(' ')}`.toLowerCase();
  const rawTokens = tokenize(query);
  const expanded = expandTokens(rawTokens);
  const matchedRawTokens = rawTokens.filter((token) => token && base.includes(String(token).toLowerCase()));
  const matchedExpandedTokens = expanded.filter((token) => token && base.includes(String(token).toLowerCase()));
  return {
    rawTokens,
    matchedRawTokens,
    matchedExpandedTokens,
    hasRawMatch: matchedRawTokens.length > 0,
    hasAnyMatch: matchedExpandedTokens.length > 0
  };
}

function scoreSource(source, query, stats) {
  let s = 0;
  s += stats.matchedRawTokens.length * 40;
  s += Math.max(0, stats.matchedExpandedTokens.length - stats.matchedRawTokens.length) * 15;

  if (hasChinese(query) && stats.hasAnyMatch) {
    if (source.lang === 'zh') s += 35;
    if ((source.tags || []).some((x) => /中文|china|zh|cn/i.test(String(x)))) s += 15;
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
  const zh = hasChinese(q);

  const dynamic = [
    {
      type: 'github',
      name: `GitHub Trending: ${topic}`,
      url: `https://github.com/topics/${topic}`,
      tags: ['github', topic, 'trending'],
      lang: 'en'
    },
    {
      type: 'github',
      name: `GitHub Search: ${q}`,
      url: `https://github.com/search?q=${encodeURIComponent(q)}`,
      tags: ['github', ...tokenize(q)],
      lang: 'en'
    },
    {
      type: 'rss',
      name: `Google News RSS: ${q}`,
      url: `https://news.google.com/rss/search?q=${encodeURIComponent(q)}`,
      tags: ['news', ...tokenize(q)],
      lang: 'en'
    },
    {
      type: 'rss',
      name: `Google News CN RSS: ${q}`,
      url: `https://news.google.com/rss/search?q=${encodeURIComponent(q)}&hl=zh-CN&gl=CN&ceid=CN:zh-Hans`,
      tags: ['news', 'cn', ...tokenize(q)],
      lang: 'zh'
    },
    {
      type: 'social',
      name: `Reddit Search RSS: ${q}`,
      url: `https://www.reddit.com/search.rss?q=${encodeURIComponent(q)}`,
      tags: ['reddit', ...tokenize(q)],
      lang: 'en'
    }
  ];

  if (zh) {
    dynamic.push({
      type: 'rss',
      name: `知乎搜索（RSSHub）: ${q}`,
      url: `https://rsshub.app/zhihu/search/${encodeURIComponent(q)}`,
      tags: ['中文', '知乎', ...tokenize(q)],
      lang: 'zh'
    });
  }

  return dynamic;
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
    tags: it.tags || [],
    lang: it.lang || null
  }));
}

export async function discoverSources(query, limit = 8) {
  const q = String(query || '').trim() || 'ai';
  const safeLimit = Number.isFinite(limit) ? Math.max(1, Math.min(50, Number(limit))) : 8;

  try {
    const relayItems = dedupeAndValidate(await relayDiscover(q, safeLimit) || []);
    if (relayItems.length) {
      return { mode: 'relay', items: relayItems.slice(0, safeLimit) };
    }
  } catch {
    // fallback below
  }

  const pool = hasChinese(q)
    ? [...injectDynamicSources(q), ...DEFAULT_ZH_SOURCES, ...DEFAULT_EN_SOURCES]
    : [...injectDynamicSources(q), ...DEFAULT_EN_SOURCES, ...DEFAULT_ZH_SOURCES];

  const ranked = dedupeAndValidate(pool)
    .map((s) => {
      const stats = getMatchStats(s, q);
      return { ...s, score: scoreSource(s, q, stats), stats };
    })
    .filter((s) => s.stats.hasAnyMatch)
    .sort((a, b) => b.score - a.score)
    .slice(0, safeLimit)
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
