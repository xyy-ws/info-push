import test from 'node:test';
import assert from 'node:assert/strict';
import { discoverSources } from '../src/ai-discovery.js';

function hasChinese(text = '') {
  return /[\u3400-\u9fff]/.test(String(text));
}

test('discover-sources: 中文关键词优先返回中文候选', async () => {
  const result = await discoverSources('人工智能 科技 资讯', 12);

  assert.equal(Array.isArray(result.items), true);
  assert.equal(result.items.length > 0, true);

  const zhItems = result.items.filter((item) => hasChinese(item.name) || (item.tags || []).some((tag) => hasChinese(tag)));
  assert.equal(zhItems.length > 0, true);

  const urls = result.items.map((item) => item.url);
  const uniqueUrls = new Set(urls);
  assert.equal(uniqueUrls.size, urls.length, 'should dedupe urls in discover results');

  for (const url of urls) {
    assert.match(url, /^https?:\/\//, `invalid url: ${url}`);
  }
});
