function nowIso() {
  return new Date().toISOString();
}

function stripXml(v = '') {
  return String(v)
    .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1')
    .replace(/<[^>]+>/g, '')
    .trim();
}

function decodeXmlEntities(v = '') {
  return String(v)
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
}

function normalizeXml(xml = '') {
  return String(xml)
    .replace(/^\uFEFF/, '')
    .replace(/<(\/?)\s*([A-Za-z_][\w.-]*):/g, '<$1');
}

function extractTag(block = '', tag = '') {
  const m = block.match(new RegExp(`<${tag}[^>]*>([\\s\\S]*?)<\\/${tag}>`, 'i'));
  return m ? stripXml(decodeXmlEntities(m[1])) : '';
}

function parseRssItems(xml = '', limit = 20) {
  const blocks = [...String(xml).matchAll(/<item\b[^>]*>([\s\S]*?)<\/item>/gi)].slice(0, limit);
  return blocks
    .map((m) => {
      const b = m[1] || '';
      const title = extractTag(b, 'title') || 'Untitled';
      const link = extractTag(b, 'link');
      const desc = extractTag(b, 'description') || extractTag(b, 'content') || extractTag(b, 'encoded');
      const pubDate = extractTag(b, 'pubDate') || extractTag(b, 'published') || extractTag(b, 'updated');
      return {
        title,
        url: link,
        summary: desc || title,
        publishedAt: pubDate ? new Date(pubDate).toISOString() : nowIso()
      };
    })
    .filter((x) => x.url);
}

function parseAtomItems(xml = '', limit = 20) {
  const blocks = [...String(xml).matchAll(/<entry\b[^>]*>([\s\S]*?)<\/entry>/gi)].slice(0, limit);
  return blocks
    .map((m) => {
      const b = m[1] || '';
      const title = extractTag(b, 'title') || 'Untitled';
      const linkTag = b.match(/<link\b[^>]*>/i)?.[0] || '';
      const altLinkTag =
        [...b.matchAll(/<link\b[^>]*>/gi)].map((x) => x[0]).find((t) => /rel\s*=\s*['"]alternate['"]/i.test(t)) ||
        linkTag;
      const href =
        altLinkTag.match(/href\s*=\s*['"]([^'"]+)['"]/i)?.[1] ||
        extractTag(b, 'id');
      const summary = extractTag(b, 'summary') || extractTag(b, 'content') || title;
      const publishedAt = extractTag(b, 'published') || extractTag(b, 'updated');
      return {
        title,
        url: decodeXmlEntities(href),
        summary,
        publishedAt: publishedAt ? new Date(publishedAt).toISOString() : nowIso()
      };
    })
    .filter((x) => x.url);
}

export function parseFeedItems(xml = '', limit = 20) {
  const normalized = normalizeXml(xml);
  const isAtom = /<feed\b/i.test(normalized) || /<entry\b/i.test(normalized);
  const isRss = /<rss\b/i.test(normalized) || /<channel\b/i.test(normalized) || /<item\b/i.test(normalized);

  if (isAtom) return parseAtomItems(normalized, limit);
  if (isRss) return parseRssItems(normalized, limit);
  return [];
}

function detectSourceType(source) {
  const url = source.url || '';
  if (url.includes('github.com') || source.type === 'github') return 'github';
  if (source.type === 'social') return 'social';
  if (source.type === 'rss' || /rss|feed\.xml|\.xml($|\?)/i.test(url)) return 'feed';
  if (/v2ex\.com\/index\.xml/i.test(url)) return 'feed';
  return 'unknown';
}

export async function fetchSourceItems(source, limit = 20, deps = {}) {
  const { fetchImpl = fetch, fetchTrendingReposByKeyword } = deps;
  const normalizedSource = { enabled: true, fetchMode: 'hybrid', ...source };
  const sourceType = detectSourceType(normalizedSource);

  if (sourceType === 'github') {
    if (typeof fetchTrendingReposByKeyword !== 'function') {
      throw new Error('github_dependency_missing');
    }
    const m = (normalizedSource.url || '').match(/topics\/([a-zA-Z0-9_-]+)/i);
    const keyword = m?.[1] || (normalizedSource.tags && normalizedSource.tags[0]) || normalizedSource.name || 'ai';
    const result = await fetchTrendingReposByKeyword(keyword, limit);
    return (result.items || []).slice(0, limit).map((it) => ({
      title: it.title,
      summary: it.summaryZh || it.summary || '暂无简介',
      url: it.url,
      publishedAt: it.updatedAt || nowIso()
    }));
  }

  if (sourceType === 'feed') {
    let resp;
    try {
      resp = await fetchImpl(normalizedSource.url, {
        headers: {
          'User-Agent': 'info-push-app/0.1',
          Accept: 'application/atom+xml, application/rss+xml, application/xml, text/xml;q=0.9, */*;q=0.8'
        }
      });
    } catch (e) {
      throw new Error(`network_error:${e?.message || 'fetch_failed'}`);
    }

    if (!resp.ok) throw new Error(`http_status_${resp.status}`);

    const body = await resp.text();
    const items = parseFeedItems(body, limit);
    if (!items.length) throw new Error('feed_empty');
    return items;
  }

  if (sourceType === 'social') {
    if (/reddit\.com/i.test(normalizedSource.url || '')) {
      const rssUrl = normalizedSource.url.includes('.rss') ? normalizedSource.url : `${normalizedSource.url.replace(/\/$/, '')}/.rss`;
      const resp = await fetchImpl(rssUrl, { headers: { 'User-Agent': 'info-push-app/0.1' } });
      if (!resp.ok) throw new Error(`social_reddit_status_${resp.status}`);
      const xml = await resp.text();
      const items = parseFeedItems(xml, limit);
      if (!items.length) throw new Error('feed_empty');
      return items;
    }
    throw new Error('social_source_unsupported');
  }

  throw new Error('source_type_unsupported');
}

export function mapProbeError(error) {
  const detail = String(error?.message || error || 'unknown_error');

  if (detail.startsWith('network_error:')) {
    return {
      error: 'source_probe_network_failed',
      detail,
      message: '添加失败：网络连接失败，请稍后重试'
    };
  }

  if (detail.startsWith('http_status_')) {
    return {
      error: 'source_probe_http_failed',
      detail,
      message: '添加失败：信息源返回异常状态码'
    };
  }

  if (detail === 'feed_empty') {
    return {
      error: 'source_probe_empty',
      detail,
      message: '添加失败：该信息源当前没有可解析内容'
    };
  }

  if (detail === 'source_type_unsupported' || detail === 'social_source_unsupported') {
    return {
      error: 'source_probe_unsupported',
      detail,
      message: '添加失败：暂不支持该信息源格式'
    };
  }

  return {
    error: 'source_probe_failed',
    detail,
    message: '添加失败：信息源测试未通过'
  };
}

export async function probeSource(source, deps = {}) {
  try {
    const items = await fetchSourceItems(source, 3, deps);
    if (!Array.isArray(items) || items.length === 0) {
      return {
        ok: false,
        ...mapProbeError(new Error('feed_empty'))
      };
    }
    return { ok: true, itemCount: items.length };
  } catch (error) {
    return {
      ok: false,
      ...mapProbeError(error)
    };
  }
}
