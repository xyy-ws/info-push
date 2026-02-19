import test from 'node:test';
import assert from 'node:assert/strict';
import { parseFeedItems, probeSource } from '../src/source-probe.js';

test('parseFeedItems: RSS should parse item list', () => {
  const rss = `<?xml version="1.0" encoding="UTF-8"?>
    <rss version="2.0"><channel>
      <item><title>新闻A</title><link>https://example.com/a</link><description>摘要A</description><pubDate>Thu, 19 Feb 2026 10:00:00 GMT</pubDate></item>
    </channel></rss>`;

  const items = parseFeedItems(rss, 5);
  assert.equal(items.length, 1);
  assert.equal(items[0].title, '新闻A');
  assert.equal(items[0].url, 'https://example.com/a');
});

test('parseFeedItems: namespaced Atom should parse entry list (v2ex style)', () => {
  const atom = `<?xml version="1.0" encoding="utf-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>V2EX</title>
      <entry>
        <title>主题A</title>
        <id>tag:v2ex.com,2026:1</id>
        <link rel="alternate" type="text/html" href="https://www.v2ex.com/t/1"/>
        <updated>2026-02-19T10:00:00Z</updated>
        <summary type="html">内容A</summary>
      </entry>
    </feed>`;

  const items = parseFeedItems(atom, 5);
  assert.equal(items.length, 1);
  assert.equal(items[0].title, '主题A');
  assert.equal(items[0].url, 'https://www.v2ex.com/t/1');
});

test('probeSource: empty feed should fail with machine-readable error', async () => {
  const fakeFetch = async () => ({
    ok: true,
    status: 200,
    text: async () => `<?xml version="1.0"?><rss><channel></channel></rss>`
  });

  const result = await probeSource(
    { name: 'empty', url: 'https://example.com/rss.xml', type: 'rss' },
    { fetchImpl: fakeFetch }
  );

  assert.equal(result.ok, false);
  assert.equal(result.error, 'source_probe_empty');
  assert.equal(result.detail, 'feed_empty');
  assert.match(result.message, /没有可解析内容/);
});
