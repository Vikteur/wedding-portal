---
id: m-4
title: "P4 Release"
---

## Description

Release: one deploy of wedding-portal, after phase 3, to a new server behind Cloudflare with an origin certificate (UD-19.b), through a pipeline that avoids the POC hazards H1, H3 to H5 and H7 to H10: its own compose project and settings, the host named only by the deployment setting PUBLIC_HOST (UD-19.n2), configuration in git, a pinned SSH host key, a pre-flight step, a health gate with a rollback that works, the three unchanged frontend bundles served by the same nginx (UD-19.n1), automated backups whose target, schedule and retention are the deployment settings BACKUP_TARGET, BACKUP_SCHEDULE and BACKUP_RETENTION_DAYS without defaults, so the release stops while any is unset (UD-19.n3), and no tokens in logs. The POC data volume is archived with a SHA-256 file into a new, dedicated, private repository (UD-20.d). Afterwards rekord-api and rekord-backend are archived read-only as a separate STOP step (UD-13).
