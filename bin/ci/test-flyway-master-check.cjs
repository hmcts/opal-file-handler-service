const { test } = require('node:test');
const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const { mkdtempSync, mkdirSync, writeFileSync, rmSync } = require('node:fs');
const { tmpdir } = require('node:os');
const { join } = require('node:path');
const { changedMasterMigrations, reconcileComment } = require('./flyway-master-check.cjs');
const path = 'src/main/resources/db/migration/ddl/V1_1__base.sql';
function fixture(t) {
  const cwd = mkdtempSync(join(tmpdir(), 'flyway-master-'));
  t.after(() => rmSync(cwd, { recursive: true, force: true }));
  const git = (...args) => execFileSync('git', args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  const write = (file, body) => { mkdirSync(join(cwd, file, '..'), { recursive: true }); writeFileSync(join(cwd, file), body); };
  const commit = () => { git('add', '.'); git('-c', 'user.name=Test', '-c', 'user.email=test@example.com', 'commit', '-qm', 'fixture'); };
  git('init', '-q', '-b', 'master'); write(path, 'original\n'); commit();
  git('update-ref', 'refs/remotes/origin/master', 'HEAD'); git('checkout', '-qb', 'feature');
  return { cwd, git, write, commit };
}
test('rejects edits and accepts a subsequent revert and new migrations', t => {
  const f = fixture(t);
  f.write(path, 'edited\n'); f.commit();
  assert.deepEqual(changedMasterMigrations(f.cwd), [path]);
  f.write(path, 'original\n'); f.write('src/main/resources/db/migration/ddl/V1_2__new.sql', 'new\n'); f.commit();
  assert.deepEqual(changedMasterMigrations(f.cwd), []);
});
for (const change of ['delete', 'rename']) test(`rejects ${change} of a master migration`, t => {
  const f = fixture(t);
  if (change === 'delete') f.git('rm', path);
  else f.git('mv', path, path.replace('base', 'renamed'));
  f.commit(); assert.deepEqual(changedMasterMigrations(f.cwd), [path]);
});
test('ignores master advancement but accepts a PR edit matching current master', t => {
  const f = fixture(t);
  f.git('checkout', 'master'); f.write(path, 'master update\n'); f.commit();
  f.git('update-ref', 'refs/remotes/origin/master', 'HEAD'); f.git('checkout', 'feature');
  assert.deepEqual(changedMasterMigrations(f.cwd), []);
  f.write(path, 'master update\n'); f.commit();
  assert.deepEqual(changedMasterMigrations(f.cwd), []);
});
test('creates one warning, updates it, and deletes it after resolution without touching other comments', () => {
  const comments = [{ id: 1, body: '<!-- flyway-master-migrations -->', user: { login: 'human' } }];
  let nextId = 2;
  const api = (endpoint, method = 'GET', body) => {
    if (method === 'GET') return [comments.slice()];
    const id = Number(endpoint.split('/').at(-1));
    if (method === 'POST') comments.push({ id: nextId++, body: body.body, user: { login: 'github-actions[bot]' } });
    if (method === 'PATCH') comments.find(c => c.id === id).body = body.body;
    if (method === 'DELETE') comments.splice(comments.findIndex(c => c.id === id), 1);
  };
  reconcileComment(api, 'owner/repo', 123, [path]);
  assert.equal(comments.length, 2); assert.ok(comments[1].body.includes(path));
  assert.match(comments[1].body, /checksum/i);
  reconcileComment(api, 'owner/repo', 123, [path, 'another.sql']);
  assert.equal(comments.length, 2); assert.ok(comments[1].body.includes('another.sql'));
  reconcileComment(api, 'owner/repo', 123, []);
  assert.deepEqual(comments.map(c => c.id), [1]);
});

test('removes all obsolete bot warnings across comment pages', () => {
  const deleted = [];
  const api = (endpoint, method = 'GET') => {
    if (method === 'GET') return [[{ id: 10, body: 'unrelated', user: { login: 'github-actions[bot]' } }],
      [20, 21].map(id => ({ id, body: '<!-- flyway-master-migrations -->', user: { login: 'github-actions[bot]' } }))];
    assert.equal(method, 'DELETE'); deleted.push(endpoint);
  };
  reconcileComment(api, 'owner/repo', 123, []);
  assert.deepEqual(deleted, ['repos/owner/repo/issues/comments/20', 'repos/owner/repo/issues/comments/21']);
});

test('CLI fails on protected edits and passes after restoration', t => {
  const { spawnSync } = require('node:child_process');
  const { chmodSync } = require('node:fs');
  const f = fixture(t);
  const executable = join(f.cwd, 'fake-bin'); mkdirSync(executable);
  const gh = join(executable, 'gh');
  // The only external boundary is GitHub; Git and the actual CLI run unchanged.
  writeFileSync(gh, '#!/usr/bin/env node\nprocess.stdout.write(process.argv.includes("GET") ? "[[]]" : "{}");\n');
  chmodSync(gh, 0o755);
  const run = () => spawnSync(process.execPath, [join(__dirname, 'flyway-master-check.cjs')], {
    cwd: f.cwd, encoding: 'utf8', env: { ...process.env, PATH: executable + ':' + process.env.PATH,
      GITHUB_REPOSITORY: 'owner/repo', PR_NUMBER: '123' },
  });
  f.write(path, 'edited\n'); f.commit();
  const rejected = run(); assert.equal(rejected.status, 1); assert.ok(rejected.stderr.includes(path));
  f.write(path, 'original\n'); f.commit();
  const restored = run(); assert.equal(restored.status, 0, restored.stderr);
});
