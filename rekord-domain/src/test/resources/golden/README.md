# Golden fixtures

Copied from the rekord-api oracle (read only), commit `ec65ae35c182e6e25f571c76d45b15a78f183c10` (ec65ae3):

- `src/test/resources/golden-set.json` -> `golden-set.json`
- `src/test/resources/golden-library.json` -> `golden-library.json` (byte for byte, except the 7 titles below)

The files are final. There is no generator in this repo (UD-16); the `note` text inside is rekord-api's.

## The two changes to `golden-set.json`

1. `source.database` is `/music/golden-src.db` (UD-19.m3, RISK-15): the recorded value was a machine path.
2. The 18 UD-19.c cases (0-based positions 82, 85, 86, 88, 90, 100, 101, 107, 108, 109, 110, 111, 115, 122, 152,
   155, 156, 161) expect `bucket` ambiguous and `auto_selected_id` null instead of auto and the id. Their
   candidates and `from_preference` are unchanged. The summary block follows: `by_bucket` auto 29, ambiguous 180,
   unmatched 3; `by_family` typo, no_artist and feat_inline.

UD-19.m6 changes no recorded expectation. Nothing else in `golden-set.json` differs from the oracle.

## The change to `golden-library.json`

The titles of the tracks with ids c89f6d9710b7, eef1a845080f, 8555a3ea5d72, 8a7d4af89b6f, 71875e38f663,
71f166562543 and cfa8ac3f386b are `openingsdans-1` to `openingsdans-7`, in that order, because they held a client name
(owner decision, 2026-10-08). None of these tracks is a candidate in any case, so no expectation changed. A re-copy from
the oracle must keep the placeholders; `GoldenFixturesTest` pins them. Nothing else in the file differs from the oracle.
