---
type: Groma Project
title: wedding-portal
groma:
  profile: architecture
description: Architecture of wedding-portal, the Quarkus rewrite of the Rekord wedding-planning backend.
---

wedding-portal is the new Java backend behind Rekord, the wedding app in which a planner organises weddings, the couple and their friends fill in their music, and a DJ matches the songs to files in their library and exports a playlist. It implements the `rekord-contract` OpenAPI spec and replaces `rekord-api` phase by phase. The code is early: the build, the REST skeleton, the error model and the track-matching core exist; most operations of the contract are not implemented yet.
