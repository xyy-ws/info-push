const GITHUB_API = 'https://api.github.com/search/repositories';

function authHeader() {
  const token = process.env.GITHUB_TOKEN || process.env.GH_TOKEN || '';
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function normalizeItem(item) {
  return {
    id: `gh-${item.id}`,
    source: 'github',
    title: item.full_name,
    summary: item.description || 'No description',
    url: item.html_url,
    stars: item.stargazers_count || 0,
    updatedAt: item.updated_at,
    language: item.language || null
  };
}

export async function fetchLatestAiRepos(limit = 10) {
  const q = encodeURIComponent('ai in:name,description,readme sort:updated-desc');
  const perPage = Math.min(Math.max(Number(limit) || 10, 1), 50);
  const url = `${GITHUB_API}?q=${q}&per_page=${perPage}`;

  try {
    const res = await fetch(url, {
      headers: {
        Accept: 'application/vnd.github+json',
        'User-Agent': 'info-push-app/0.1',
        ...authHeader()
      }
    });

    if (!res.ok) {
      throw new Error(`github_status_${res.status}`);
    }

    const data = await res.json();
    const items = Array.isArray(data.items) ? data.items.map(normalizeItem) : [];
    return { items, provider: 'github', live: true };
  } catch (error) {
    return {
      provider: 'github',
      live: false,
      error: String(error?.message || error),
      items: [
        {
          id: 'gh-fallback-1',
          source: 'github',
          title: 'fallback/sample-ai-repo',
          summary: 'Fallback item when GitHub API is unavailable or rate-limited.',
          url: 'https://github.com/topics/ai',
          stars: 0,
          updatedAt: new Date().toISOString(),
          language: null
        }
      ]
    };
  }
}
