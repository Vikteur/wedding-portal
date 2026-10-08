---
type: C4 Component
title: Metric label filter
status: stable
groma:
  id: metric-label-filter
  parent: wedding-portal-api
  code:
    - scanner: java
      file: application/src/main/java/app/rekord/application/config/MetricLabels.java
  technology: Micrometer MeterFilter (CDI producer)
description: Keeps request paths out of the metric labels of requests the client resets.
---

A CDI-produced Micrometer `MeterFilter` that rewrites the `uri` tag of every `http.server.*` meter with `status=RESET` to UNKNOWN. With `quarkus.micrometer.binder.http-server.suppress4xx-errors=true` an untemplated answer of 400 or more is already labelled UNKNOWN, but Quarkus records a request the client reset before it was answered through a hook that hard-codes the status and skips the suppression, so such a request would carry its raw path (an id or personal data) into `/q/metrics`. An untemplated answer below 400 that is not a 404 or a 3xx still keeps its raw path; no route answers like that today.
