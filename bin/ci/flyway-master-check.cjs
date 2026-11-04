const { execFileSync } = require('node:child_process');

const MIGRATIONS = 'src/main/resources/db/migration';
const MARKER = '<!-- flyway-master-migrations -->';

function changedMasterMigrations(cwd = process.cwd()) {
  const git = (...args) => execFileSync('git', args, { cwd, encoding: 'utf8' });
  const base = git('merge-base', 'origin/master', 'HEAD').trim();
  // Limit protection to paths touched by the PR, so master-only changes are ignored.
  const touched = git('diff', '--no-renames', '--name-only', '-z', base, 'HEAD', '--', MIGRATIONS)
    .split('\0').filter(Boolean);
  const tree = ref => new Map(git('ls-tree', '-r', '-z', ref, '--', MIGRATIONS)
    .split('\0').filter(Boolean).map(entry => {
      const tab = entry.indexOf('\t');
      return [entry.slice(tab + 1), entry.slice(0, tab)];
    }));
  const master = tree('origin/master');
  const head = tree('HEAD');
  return touched.filter(path => /\/V[^/]*\.sql$/.test(path)
    && master.has(path) && master.get(path) !== head.get(path));
}

function reconcileComment(api, repository, prNumber, paths) {
  const endpoint = `repos/${repository}/issues/${prNumber}/comments`;
  const comments = api(endpoint).flat().filter(comment =>
    comment.user.login === 'github-actions[bot]' && (comment.body || '').includes(MARKER));
  if (paths.length) {
    const body = [MARKER, '### Existing Flyway migrations must remain unchanged', '',
      'This check failed because this PR edits, deletes or renames migrations already on master.',
      'Flyway records checksums for applied migrations. Changing them can cause validation failures or leave environments with inconsistent schemas.',
      'Restore these files to match master and put the required SQL changes in a new versioned migration:', '',
      ...paths.map(path => `- \`${path}\``), '',
      'This comment will be removed automatically when the protected files match master again.'].join('\n');
    if (comments.length) api(`repos/${repository}/issues/comments/${comments[0].id}`, 'PATCH', { body });
    else api(endpoint, 'POST', { body });
  }
  // Remove obsolete warnings, including any duplicate marker comments from previous runs.
  for (const comment of comments.slice(paths.length ? 1 : 0)) {
    api(`repos/${repository}/issues/comments/${comment.id}`, 'DELETE');
  }
}

function githubApi(endpoint, method = 'GET', body) {
  const args = ['api', endpoint, '--method', method];
  if (method === 'GET') args.push('--paginate', '--slurp');
  if (body) args.push('--input', '-');
  const output = execFileSync('gh', args, { encoding: 'utf8', input: body ? JSON.stringify(body) : undefined });
  return output.trim() ? JSON.parse(output) : undefined;
}

if (require.main === module) {
  const paths = changedMasterMigrations();
  // Reconcile before failing, and before the duplicate checker can fail or rename files.
  reconcileComment(githubApi, process.env.GITHUB_REPOSITORY, process.env.PR_NUMBER, paths);
  if (paths.length) {
    console.error('Existing master Flyway migrations must remain unchanged:\n' + paths.join('\n'));
    process.exitCode = 1;
  }
}

module.exports = { changedMasterMigrations, reconcileComment };
