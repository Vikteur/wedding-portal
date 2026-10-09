// Parsing and rendering for the retro scripts (P2): retro-evidence.sh and retro-commit.sh gather the data with git, gh
// and archon, and call this file for the parts that need more than a line of shell.
//   node retro.js render <meta.json> <out.md>    the evidence a retro is written from
//   node retro.js gate <umbrella> <folder> <id8> checks lessons-learned.md and the ADRs; exit 1 with the reasons
//   node retro.js index <umbrella>               regenerates the index in docs/retro/README.md
//   node retro.js sweep <umbrella> <archon home> <runs.json>
//                                                copies every finished Archon run to docs/retro/<TASK>/runs/<id8>/
//   node retro.js note <get.json> <reason> <action>
//                                                keeps why a run was stopped beside its artifacts (stop-reason.json)
//   node retro.js pending <runs.json> <workflow> the ids of that workflow's stopped (cancelled) runs
//   node retro.js stopped <add|refresh> <get.json> <transcript> <runs.json> <umbrella>
//                                                writes a stopped run's section into its lessons-learned.md and prints
//                                                the retro folder, or nothing when the run has a section to keep
// Model-written files may have CRLF line endings, so every file is read with \r stripped.
'use strict';
const fs = require('fs');
const path = require('path');

const LESSONS_H3 = ['What happened', 'Decisions', 'What went well', 'What dragged', 'Lessons', 'Actions'];
// A run archon ended outright (a cancel, an abandon, a rejected approval gate) never reached the retro agent: its
// section is written by script and has only these headings.
const STOPPED = ['cancelled', 'rejected'];
const STOPPED_H3 = ['What happened', 'Why it stopped'];
const ADR_H2 = ['Status', 'Context', 'Decision', 'Alternatives considered', 'Consequences', 'Evidence'];
const ADR_FILE = /^ADR-(\d{2})-[a-z0-9]+(-[a-z0-9]+)*\.md$/;
const SUMMARY_CHARS = 1200;

const read = f => { try { return fs.readFileSync(f, 'utf8').replace(/\r/g, ''); } catch { return null; } };
const json = f => { const s = read(f); if (!s || !s.trim()) return null; try { return JSON.parse(s); } catch { return null; } };
const short = s => (s || '').slice(0, 7);
const oneLine = s => (s || '').replace(/\s+/g, ' ').trim();
const text = c => typeof c === 'string' ? c
  : Array.isArray(c) ? c.map(p => typeof p === 'string' ? p : p && p.text || '').join('') : '';
const clip = (s, n) => s.length > n ? s.slice(0, n) + ' …' : s;

// ---------- render ----------
function timeline(events) {
  const lines = [];
  const agents = new Map(); // step -> {last, tools, runs}
  let current = null;
  for (const e of events) {
    switch (e.type) {
      case 'node_start': {
        current = e.step;
        if (e.execution && e.execution.node && e.execution.node.kind === 'agent') {
          const a = agents.get(e.step) || { last: '', tools: 0, runs: 0 };
          a.runs++; agents.set(e.step, a);
        }
        break;
      }
      case 'node_complete':
        lines.push(`- ${e.step}: completed (${Math.round((e.duration_ms || 0) / 1000)}s)`);
        if (current === e.step) current = null;
        break;
      case 'node_error':
        lines.push(`- ${e.step}: failed: ${oneLine(e.error || e.content)}`);
        if (current === e.step) current = null;
        break;
      case 'node_skipped': {
        const c = e.cause || {};
        const why = c.kind ? `${c.kind}: ${c.expr || c.origin || ''}`.replace(/: $/, '') : oneLine(e.content);
        lines.push(`- ${e.step}: skipped (${why})`);
        break;
      }
      case 'workflow_error':
        lines.push(`- workflow: failed: ${oneLine(e.error || e.content)}`);
        break;
      case 'assistant': {
        const a = current && agents.get(current);
        const t = text(e.content).trim();
        if (a && t) a.last = t;
        break;
      }
      case 'tool': {
        const a = current && agents.get(current);
        if (a) a.tools++;
        break;
      }
    }
  }
  return { lines, agents };
}

function render(metaFile, out) {
  const m = json(metaFile);
  const md = [];
  md.push(`# Retro evidence — run ${m.run.slice(0, 8)}`, '');
  md.push(`- Request: ${oneLine(m.request)}`);
  md.push(`- Ticket: ${m.task || 'none named in the request'}`);
  md.push(`- Branch: ${m.branch || 'unknown'}`);
  md.push(`- Pull request: ${m.pr || 'none'}`);
  md.push(`- Retro folder: ${m.dir} (next ADR number: ${m.next_adr})`, '');

  const raw = read(m.transcript);
  const events = (raw || '').split('\n').map(l => { try { return JSON.parse(l); } catch { return null; } }).filter(Boolean);
  const { lines, agents } = timeline(events);
  md.push('## Node timeline', '');
  md.push(...(lines.length ? lines : ['Transcript unavailable: no node events were found.']), '');

  md.push('## Agent summaries', '', 'The last message of each agent node, as the agent wrote it.', '');
  if (!agents.size) md.push('None found.', '');
  for (const [step, a] of agents) {
    const runs = a.runs > 1 ? `, ${a.runs} runs` : '';
    md.push(`### ${step} (${a.tools} tool calls${runs})`, '', a.last ? clip(a.last, SUMMARY_CHARS) : '(no message)', '');
  }

  const pr = json(m.pr_json);
  md.push('## Commits', '');
  const commits = pr && Array.isArray(pr.commits) && pr.commits.length
    ? pr.commits.map(c => `- ${short(c.oid)} ${oneLine(c.messageHeadline)}`)
    : (read(m.commits) || '').split('\n').filter(Boolean).map(l => `- ${l}`);
  md.push(...(commits.length ? commits : ['None found.']), '');

  md.push('## Pull request', '');
  if (!m.pr) md.push('No pull request was opened for this branch.', '');
  else if (!pr) md.push(`Unavailable: \`gh pr view ${m.pr}\` failed.`, '');
  else {
    md.push(`- #${pr.number} ${oneLine(pr.title)}`);
    const merge = pr.mergeCommit && pr.mergeCommit.oid;
    md.push(`- State: ${pr.state}${pr.mergedAt ? ` (merged ${pr.mergedAt} as ${short(merge)})` : ''}, head ${short(pr.headRefOid)}`, '');
    md.push('### Reviews', '');
    const reviews = (pr.reviews || []).map(r => `- ${r.author && r.author.login} (${r.state}): ${oneLine(r.body) || '(no text)'}`);
    md.push(...(reviews.length ? reviews : ['None.']), '');
    md.push('### Comments', '');
    const comments = (pr.comments || []).map(c => `- ${c.author && c.author.login}: ${oneLine(c.body)}`);
    md.push(...(comments.length ? comments : ['None.']), '');
  }

  md.push('## CI runs', '');
  const runs = json(m.runs_json);
  if (!runs) md.push(`Unavailable: \`gh run list --branch ${m.branch}\` failed.`, '');
  else md.push(...(runs.length ? runs.map(r =>
    `- ${r.databaseId} ${r.conclusion || r.status} ${r.workflowName} ${short(r.headSha)} (${r.createdAt})`) : ['None.']), '');

  md.push(`## Artifacts (${m.artifacts_dir})`, '');
  const arts = (read(m.artifacts) || '').split('\n').filter(Boolean).map(l => `- ${l}`);
  md.push(...(arts.length ? arts : ['None.']), '');
  fs.writeFileSync(out, md.join('\n'));
}

// ---------- gate ----------
// sections(lines, marker): heading text -> body lines, for the headings that start with marker ('### ' or '## ').
function sections(lines, marker) {
  const map = new Map(); let cur = null;
  for (const l of lines) {
    if (l.startsWith(marker)) { cur = l.slice(marker.length).trim(); map.set(cur, []); }
    else if (cur !== null) map.get(cur).push(l);
  }
  return map;
}
const filled = body => body.some(l => l.trim() !== '');

function gate(home, dir, id8) {
  const errors = [];
  const folder = path.join(home, dir);
  const lessons = read(path.join(folder, 'lessons-learned.md'));
  if (lessons === null) errors.push(`${dir}/lessons-learned.md is missing`);
  else {
    const lines = lessons.split('\n');
    const start = lines.findIndex(l => l.startsWith(`## Run ${id8} `) || l === `## Run ${id8}`);
    if (start < 0) errors.push(`lessons-learned.md has no "## Run ${id8} — <date> — <outcome>" section for this run`);
    else {
      let end = lines.findIndex((l, i) => i > start && l.startsWith('## '));
      if (end < 0) end = lines.length;
      const secs = sections(lines.slice(start + 1, end), '### ');
      for (const h of STOPPED.includes(outcomeOf(lines[start])) ? STOPPED_H3 : LESSONS_H3) {
        if (!secs.has(h)) errors.push(`run ${id8}: "### ${h}" is missing`);
        else if (!filled(secs.get(h))) errors.push(`run ${id8}: "### ${h}" is empty (write "None." when there is nothing)`);
      }
    }
  }
  const adrDir = path.join(folder, 'adr');
  const adrs = fs.existsSync(adrDir) ? fs.readdirSync(adrDir).sort() : [];
  for (const f of adrs) {
    const m = ADR_FILE.exec(f);
    if (!m) { errors.push(`adr/${f} is not named ADR-NN-<slug>.md`); continue; }
    const lines = read(path.join(adrDir, f)).split('\n');
    const h1 = lines.find(l => l.trim() !== '') || '';
    if (!new RegExp(`^# ADR-${m[1]}: \\S`).test(h1)) errors.push(`adr/${f}: the first line is not "# ADR-${m[1]}: <title>"`);
    const secs = sections(lines, '## ');
    for (const h of ADR_H2) {
      if (!secs.has(h)) errors.push(`adr/${f}: "## ${h}" is missing`);
      else if (!filled(secs.get(h))) errors.push(`adr/${f}: "## ${h}" is empty`);
    }
    if (lessons !== null && !lessons.includes(`adr/${f}`)) errors.push(`adr/${f} is not linked from lessons-learned.md`);
  }
  if (errors.length) {
    console.error(`The retro in ${dir} is incomplete:\n- ${errors.join('\n- ')}`);
    process.exit(1);
  }
}

// ---------- index ----------
const START = '<!-- retro-index:start -->';
const END = '<!-- retro-index:end -->';

function index(home) {
  const base = path.join(home, 'docs', 'retro');
  const readme = path.join(base, 'README.md');
  const raw = fs.readFileSync(readme, 'utf8');
  const eol = raw.includes('\r\n') ? '\r\n' : '\n';
  const text = raw.replace(/\r/g, '');
  const a = text.indexOf(START), b = text.indexOf(END);
  if (a < 0 || b < a) { console.error(`${readme} has no ${START} ... ${END} block`); process.exit(1); }
  const names = fs.readdirSync(base, { withFileTypes: true }).filter(d => d.isDirectory()).map(d => d.name)
    .filter(n => fs.existsSync(path.join(base, n, 'lessons-learned.md')))
    .sort((x, y) => x.localeCompare(y, 'en', { numeric: true }));
  const lines = [];
  for (const n of names) {
    const runs = read(path.join(base, n, 'lessons-learned.md')).split('\n').filter(l => l.startsWith('## Run '));
    const last = runs.length ? ` — last: ${runs[runs.length - 1].slice(3).trim()}` : '';
    lines.push(`- [${n}](${n}/lessons-learned.md)${last}`);
    const adrDir = path.join(base, n, 'adr');
    const adrs = fs.existsSync(adrDir) ? fs.readdirSync(adrDir).filter(f => ADR_FILE.test(f)).sort() : [];
    for (const f of adrs) {
      const h1 = (read(path.join(adrDir, f)).split('\n').find(l => l.trim() !== '') || '').replace(/^#\s*/, '').trim();
      lines.push(`  - [${h1 || f}](${n}/adr/${f})`);
    }
  }
  const body = lines.length ? lines.join('\n') : '_No retro yet._';
  const next = `${text.slice(0, a)}${START}\n${body}\n${text.slice(b)}`;
  fs.writeFileSync(readme, next.replace(/\n/g, eol));
}

// ---------- stopped runs ----------
const TASK = /task-[0-9]+(\.[0-9]+)*/i;
const runList = j => !j ? [] : Array.isArray(j) ? j : j.runs || [];
const artifactsRoot = g => (g.terminal_record && g.terminal_record.artifacts && g.terminal_record.artifacts.root)
  || path.join(g.output_root, 'artifacts', 'runs', g.id);
const outcomeOf = header => header.split(' — ').pop().trim();
const runJson = f => { const g = json(f); if (!g) { console.error(`${f}: not a run`); process.exit(1); } return g; };

function note(getFile, reason, action) {
  const g = runJson(getFile);
  const root = artifactsRoot(g);
  fs.mkdirSync(root, { recursive: true });
  const out = path.join(root, 'stop-reason.json');
  fs.writeFileSync(out, JSON.stringify({ reason, action, status: g.status, at: new Date().toISOString() }) + '\n');
  console.log(out);
}

// Oldest first, so the sections land in the order the runs happened (archon lists the newest first).
function pending(runsFile, workflow) {
  const runs = runList(json(runsFile)).filter(r => r.status === 'cancelled' && r.workflow_name === workflow);
  runs.sort((x, y) => String(x.started_at || '').localeCompare(String(y.started_at || '')));
  for (const r of runs) console.log(r.id);
}

// Where the run was when it stopped: the node archon left running, else the last node started and never finished.
function stoppedAt(g, events) {
  const nodes = (g.terminal_record && g.terminal_record.nodes) || [];
  const running = nodes.find(n => n.state === 'running');
  if (running) return running.node_id;
  const open = new Set();
  for (const e of events) {
    if (e.type === 'node_start') open.add(e.step);
    else if (['node_complete', 'node_error', 'node_skipped'].includes(e.type)) open.delete(e.step);
  }
  return [...open].pop();
}

function whyItStopped(g, kept, gate) {
  if (kept && kept.reason) {
    const how = kept.action === 'reject' ? 'rejected at the approval gate'
      : kept.action === 'cancel' ? 'cancelled' : 'given after the run had stopped';
    return [oneLine(kept.reason), '', `(Recorded by \`stop-run.sh\` at ${kept.at}: ${how}; the run was ${kept.status}.)`];
  }
  if (gate) return [`Rejected at the approval gate \`${gate.step}\`: ${oneLine(gate.content) || '(no reason given)'}`];
  return ['No reason was recorded: the run was cancelled or abandoned without `stop-run.sh`. The timeline above shows',
    `how far it got; \`archon workflow logs ${g.id}\` has the full transcript.`];
}

// mode add: a run that already has a section is left alone (the sweep). mode refresh: a stopped-run section is
// rewritten from the current evidence, e.g. with a reason given later; a full retro section is still left alone.
function stopped(mode, getFile, transcript, runsFile, home) {
  const g = runJson(getFile);
  if (g.status !== 'cancelled') {
    console.error(`Run ${g.id.slice(0, 8)} has not been stopped (status ${g.status}); only a cancelled run is documented here.`);
    process.exit(1);
  }
  const id8 = g.id.slice(0, 8);
  const task = ((g.user_message || '').match(TASK) || [''])[0].toUpperCase();
  const name = task || `run-${id8}`;
  const dir = `docs/retro/${name}`;
  const file = path.join(home, dir, 'lessons-learned.md');
  const raw = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null;
  const eol = raw && raw.includes('\r\n') ? '\r\n' : '\n';
  const lines = raw === null ? [`# Retro — ${name}`] : raw.replace(/\r/g, '').replace(/\n+$/, '').split('\n');
  const start = lines.findIndex(l => l.startsWith(`## Run ${id8} `) || l === `## Run ${id8}`);
  if (start >= 0 && (mode !== 'refresh' || !STOPPED.includes(outcomeOf(lines[start])))) return;

  const events = (read(transcript) || '').split('\n').map(l => { try { return JSON.parse(l); } catch { return null; } })
    .filter(Boolean);
  const gate = events.filter(e => e.type === 'gate_decision' && e.decision === 'rejected').pop();
  const kept = json(path.join(artifactsRoot(g), 'stop-reason.json'));
  const outcome = gate || (kept && kept.action === 'reject') ? 'rejected' : 'cancelled';
  const ended = g.completed_at || g.last_activity_at || g.started_at || new Date().toISOString();
  const at = stoppedAt(g, events);
  const gateNode = (g.metadata && g.metadata.approval && g.metadata.approval.nodeId) || (gate && gate.step);
  const next = runList(json(runsFile)).filter(r => r.adopted_from_run_id === g.id).map(r => r.id.slice(0, 8));

  const md = [`## Run ${id8} — ${String(ended).slice(0, 10)} — ${outcome}`, '', '### What happened'];
  md.push(`- Request: ${oneLine(g.user_message) || '(none)'}`);
  md.push(`- Workflow: ${g.workflow_name}, run ${g.id}, started ${g.started_at || 'unknown'}, ended ${ended}.`);
  md.push(`- Stopped at: ${at ? at + (at === gateNode ? ' (approval gate)' : '') : 'unknown'}`);
  if (next.length) md.push(`- Superseded by run ${next.join(', run ')}, which adopted this one.`);
  const { lines: tl } = timeline(events);
  md.push('- Node timeline:', ...(tl.length ? tl.map(l => `  ${l}`) : ['  - no node events in the transcript']));
  md.push('', '### Why it stopped', ...whyItStopped(g, kept, gate));

  let out;
  if (start >= 0) {
    let end = lines.findIndex((l, i) => i > start && l.startsWith('## '));
    if (end < 0) end = lines.length;
    out = [...lines.slice(0, start), ...md, ...(end < lines.length ? ['', ...lines.slice(end)] : [])];
  } else out = [...lines, '', ...md];
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, (out.join('\n') + '\n').replace(/\n/g, eol));
  console.log(dir);
}

// ---------- archive every finished Archon run ----------
// The Archon run folders stay outside the repo and temporary. The sweep copies each finished run's key files, logs and
// transcript to docs/retro/<TASK>/runs/<id8>/ (docs/retro/unlinked/runs/<id8>/ when no ticket is found), with a run.json
// manifest. Dotfiles and dot-folders (.archon/..., .pr-number) are never copied; a file that is not there is skipped.
const { spawnSync } = require('child_process');
const RUN_FILES = ['ticket.md', 'plan.md', 'pr-body.md', 'ci-diagnosis.md', 'retro-evidence.md',
  'review/consolidated-review.md', 'review/fix-report.md', 'review/scope.md',
  'retro/transcript.jsonl', 'retro/runs.json', 'retro/meta.json', 'retro/commits.txt', 'retro/pr.json',
  'retro/artifacts.txt'];
const TERMINAL = { workflow_complete: 'completed', workflow_error: 'failed' };
const TASK_ID = /task-[0-9]+(?:\.[0-9]+)*/i;
const OWNER = 'Vikteur';

function logsUnder(root, rel = '') {
  let entries;
  try { entries = fs.readdirSync(path.join(root, rel), { withFileTypes: true }); } catch { return []; }
  return entries.filter(e => !e.name.startsWith('.')).flatMap(e => {
    const r = rel ? `${rel}/${e.name}` : e.name;
    return e.isDirectory() ? logsUnder(root, r) : e.isFile() && e.name.endsWith('.log') ? [r] : [];
  });
}
function copyRunFiles(runDir, target) {
  for (const f of new Set([...RUN_FILES, ...logsUnder(runDir)])) {
    const src = path.join(runDir, f);
    if (!fs.existsSync(src) || !fs.statSync(src).isFile()) continue;
    fs.mkdirSync(path.dirname(path.join(target, f)), { recursive: true });
    fs.copyFileSync(src, path.join(target, f));
  }
}
// bash runs the command so that a stub or a shim on PATH is found the same way the scripts find it.
function viaBash(cmd, args) {
  const r = spawnSync('bash', ['-c', 'exec "$@"', 'x', cmd, ...args], { encoding: 'utf8' });
  return r.status === 0 ? r.stdout : null;
}
// What the log says: workflow_start (name, request, start) and the last terminal event not followed by a resume.
function readLog(file) {
  const events = (read(file) || '').split('\n').map(l => { try { return JSON.parse(l); } catch { return null; } })
    .filter(Boolean);
  const start = events.find(e => e.type === 'workflow_start') || {};
  let state = null, ended = null;
  for (const e of events) {
    const t = TERMINAL[e.type] || (/cancel|abandon/.test(e.type || '') ? 'cancelled' : null);
    if (t) { state = t; ended = e.ts || null; }
    else if (e.type === 'workflow_resume') { state = null; ended = null; }
  }
  return { workflow: start.workflow_name || null, request: start.content || '', started: start.ts || null, state, ended };
}
function taskOf(s) { const m = TASK_ID.exec(s || ''); return m ? m[0].toUpperCase() : null; }
function prNumber(runDir, request) {
  const f = (read(path.join(runDir, '.pr-number')) || '').trim();
  if (/^[0-9]+$/.test(f)) return f;
  const m = /(?:\bPR|pull request|pull)\s*#?([0-9]+)/i.exec(request) || /\/pull\/([0-9]+)/.exec(request)
    || /#([0-9]+)/.exec(request);
  return m ? m[1] : null;
}
// The ticket of a run: ticket.md, else its pull request (branch, title, body), else its request, else none.
function link(repo, runDir, request) {
  const pr = prNumber(runDir, request);
  const ticket = taskOf(read(path.join(runDir, 'ticket.md')));
  if (ticket) return { ticket, rule: 'ticket.md', pr };
  if (pr) {
    const out = viaBash('gh', ['pr', 'view', pr, '--repo', `${OWNER}/${repo}`, '--json', 'headRefName,title,body']);
    let d = null; try { d = out && JSON.parse(out); } catch { /* not JSON */ }
    const t = d && (taskOf(d.headRefName) || taskOf(d.title) || taskOf(d.body));
    if (t) return { ticket: t, rule: 'pr', pr };
  }
  const fromRequest = taskOf(request);
  if (fromRequest) return { ticket: fromRequest, rule: 'request', pr };
  return { ticket: null, rule: 'unlinked', pr };
}
// Where this run is archived already: its folder under docs/retro and its manifest, or null.
function archivedAt(home, id8) {
  const base = path.join(home, 'docs', 'retro');
  let names;
  try { names = fs.readdirSync(base); } catch { return null; }
  for (const n of names) {
    const manifest = json(path.join(base, n, 'runs', id8, 'run.json'));
    if (manifest) return { dir: `docs/retro/${n}/runs/${id8}`, name: n, manifest };
  }
  return null;
}
// Prints {"archived":[...],"paths":[...]} for the shell to commit: paths are every folder written or removed.
function sweep(home, archonHome, runsFile) {
  const archonState = new Map(runList(json(runsFile)).map(r => [r.id, r.status]));
  const attrFile = path.join(home, 'docs', 'tokenomics', 'attribution.json');
  const attribution = json(attrFile) || {};
  let attributionChanged = false;
  const archived = [], paths = [];
  const base = path.join(archonHome, 'workspaces', OWNER);
  let repos = [];
  try { repos = fs.readdirSync(base, { withFileTypes: true }).filter(d => d.isDirectory()).map(d => d.name).sort(); } catch { /* none */ }
  for (const repo of repos) {
    const logs = path.join(base, repo, 'logs');
    const arts = path.join(base, repo, 'artifacts', 'runs');
    const ids = new Set();
    try { for (const f of fs.readdirSync(logs)) if (f.endsWith('.jsonl')) ids.add(f.slice(0, -6)); } catch { /* none */ }
    try { for (const f of fs.readdirSync(arts, { withFileTypes: true })) if (f.isDirectory()) ids.add(f.name); } catch { /* none */ }
    for (const id of [...ids].sort()) {
      const logFile = path.join(logs, `${id}.jsonl`);
      const info = fs.existsSync(logFile) ? readLog(logFile)
        : { workflow: null, request: '', started: null, state: null, ended: null };
      const status = archonState.get(id);
      let state = info.state;
      if (status === 'running') state = null;
      else if (!state && ['completed', 'failed', 'cancelled'].includes(status)) state = status;
      if (!state) continue; // not finished (or unknown): the next sweep looks again
      const id8 = id.slice(0, 8);
      const old = archivedAt(home, id8);
      const same = old && old.manifest.run === id && old.manifest.state === state;
      if (same && old.name !== 'unlinked') continue; // archived in this state, and linked: nothing to do
      const runDir = path.join(arts, id);
      const l = link(repo, runDir, info.request);
      const name = l.ticket || 'unlinked';
      if (same && old.name === name) continue;
      const rel = `docs/retro/${name}/runs/${id8}`;
      const target = path.join(home, rel);
      if (old && old.dir !== rel) { fs.rmSync(path.join(home, old.dir), { recursive: true, force: true }); paths.push(old.dir); }
      if (fs.existsSync(runDir)) copyRunFiles(runDir, target);
      fs.mkdirSync(target, { recursive: true });
      if (fs.existsSync(logFile)) fs.copyFileSync(logFile, path.join(target, 'transcript.jsonl'));
      const tokenReport = l.ticket ? `docs/tokenomics/ticket-token-reports/${l.ticket}.md` : null;
      fs.writeFileSync(path.join(target, 'run.json'), JSON.stringify({
        run: id, repo, workflow: info.workflow, started: info.started, ended: info.ended, state,
        ticket: l.ticket, pr: l.pr ? Number(l.pr) : null, rule: l.rule, tokenReport }, null, 2) + '\n');
      if (l.rule === 'pr' && !(id in attribution)) { attribution[id] = l.ticket; attributionChanged = true; }
      paths.push(rel);
      archived.push({ run: id, dir: rel, ticket: l.ticket, rule: l.rule, state });
    }
  }
  if (attributionChanged) fs.mkdirSync(path.dirname(attrFile), { recursive: true });
  if (attributionChanged) fs.writeFileSync(attrFile, JSON.stringify(attribution, null, 2) + '\n');
  console.log(JSON.stringify({ archived, paths }));
}

const [cmd, ...args] = process.argv.slice(2);
if (cmd === 'render') render(...args);
else if (cmd === 'gate') gate(...args);
else if (cmd === 'index') index(...args);
else if (cmd === 'sweep') sweep(...args);
else if (cmd === 'note') note(...args);
else if (cmd === 'pending') pending(...args);
else if (cmd === 'stopped') stopped(...args);
else { console.error('usage: retro.js render|gate|index|sweep|note|pending|stopped ...'); process.exit(2); }
