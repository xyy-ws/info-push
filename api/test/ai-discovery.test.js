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

test('discover-sources: 中文加权不应替代基础匹配', async () => {
  const result = await discoverSources('量化交易', 20);

  assert.equal(result.items.length > 0, true);
  const joined = result.items.map((item) => `${item.name} ${item.url} ${(item.tags || []).join(' ')}`.toLowerCase());
  const unrelatedZh = joined.some((text) => text.includes('机器之心') || text.includes('qbitai'));
  assert.equal(unrelatedZh, false, 'unrelated zh-only ai sources should not be admitted without token match');
});

test('discover-sources: 常见类目词仍保留合理召回', async () => {
  const result = await discoverSources('开源 编程', 12);

  assert.equal(result.items.length > 0, true);
  const joined = result.items.map((item) => `${item.name} ${(item.tags || []).join(' ')}`).join(' ').toLowerCase();
  assert.equal(/开源|编程|开发|programming|developer/.test(joined), true);
});
