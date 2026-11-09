# AI Explanation UI Smoke Test

Status: **OPEN** – the screenshot gate is not closed until every row below is
completed by a person against the real screenshots/emulator. Nothing here has
been verified yet; do not record a result that was not observed.

Reference screenshots (repo root): `explanation_success_light.png`,
`explanation_success_dark.png`, `explanation_large_font.png`,
`explanation_insufficient.png`, `explanation_error.png`.

## Checklist (apply to every screenshot / state)

- [ ] No text clipping
- [ ] No overlapping elements
- [ ] Factor bullets align
- [ ] Disclaimer remains readable
- [ ] Dark-theme contrast is sufficient
- [ ] Large font remains scrollable
- [ ] Insufficient state is visually neutral
- [ ] Error state is distinguishable without raw technical details
- [ ] Positive/negative meaning does not rely only on colour
- [ ] Retry button has adequate size and separation

## Environment available for the review

Emulator `Medium_Phone_3` (`emulator-5554`), Android API 37. Captured states:
light/dark, font scale 1.0/1.5, portrait/landscape. Text-level checks (expected
copy, no internal fields, request ID only on error, one backend request across
config changes) passed; the visual items below were not reviewed because the
preparing assistant cannot view images.

## Results

| Screenshot / state | Emulator model | API level | Theme | Font scale | Orientation | Result | Observed defect |
|---|---|---|---|---|---|---|---|
| Success (light) | _TBD_ | _TBD_ | Light | 1.0 | Portrait | _Pending_ | – |
| Success (dark) | _TBD_ | _TBD_ | Dark | 1.0 | Portrait | _Pending_ | – |
| Success (large font) | _TBD_ | _TBD_ | Light | 1.3+ | Portrait | _Pending_ | – |
| Success (large font) | _TBD_ | _TBD_ | Light | 1.3+ | Landscape | _Pending_ | – |
| Insufficient evidence | _TBD_ | _TBD_ | Light | 1.0 | Portrait | _Pending_ | – |
| Fallback (degraded) success | _TBD_ | _TBD_ | Light | 1.0 | Portrait | _Pending_ | – |
| Error + retry | _TBD_ | _TBD_ | Light | 1.0 | Portrait | _Pending_ | – |

Result values: `Pass`, `Fail`, `Pending`.

## Milestone status

- Functional gate: PASS
- AI-safety gate: PASS
- Automated UI gate: PASS
- Visual-accessibility gate: **PENDING**

Layout dumps confirm text existence and semantics only, not contrast, clipping,
alignment or touch-target size, so they are not accepted as visual evidence.

## Final Result

- Success light: Pending
- Success dark: Pending
- Large font: Pending
- Insufficient evidence: Pending
- Technical error: Pending

Overall visual gate: Pending external visual review

Reviewed by: _not yet reviewed_
Review date: _not yet reviewed_

## Notes

- The fallback response uses the same fields as a normal response
  (`evidenceSentiment: "neutral"`), so it should render identically to a
  normal success state. Internal metadata (`fallbackUsed`, etc.) is not shown.
- Record every defect with a short description and screenshot name; leave the
  gate open until all rows are `Pass`.

