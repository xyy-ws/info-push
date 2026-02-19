import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';

const root = new URL('./src/', import.meta.url).pathname;
const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8'
};

const server = http.createServer(async (req, res) => {
  try {
    let p = req.url || '/';
    if (p === '/' || p === '') p = '/github-acceptance.html';
    const safe = normalize(p).replace(/^\/+/, '');
    const full = join(root, safe);
    const data = await readFile(full);
    res.writeHead(200, { 'content-type': mime[extname(full)] || 'text/plain; charset=utf-8' });
    res.end(data);
  } catch {
    res.writeHead(404, { 'content-type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ ok: false, error: 'not_found' }));
  }
});

const port = Number(process.env.PORT || 8788);
server.listen(port, () => console.log(`[info-push-web] listening on :${port}`));
