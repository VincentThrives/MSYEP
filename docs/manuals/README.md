# MSYEP User Manuals

One manual per login type. Each covers every screen that role can reach, every field on every form,
and the common end-to-end workflows — illustrated with real screenshots of the running app.

| Manual | For | Pages |
|---|---|---|
| `MSYEP-User-Manual-Admin-Console.pdf` | Super Admin, Admin | 22 |
| `MSYEP-User-Manual-Staff-Portal.pdf` | Staff | 8 |
| `MSYEP-User-Manual-Zone-Portal.pdf` | Zone / franchise partners | 8 |
| `MSYEP-User-Manual-Center-Portal.pdf` | Colleges and hostels | 9 |
| `MSYEP-User-Manual-Finance-Wing.pdf` | Finance team | 6 |
| `MSYEP-User-Manual-Student-Portal.pdf` | Students | 9 |

## Rebuilding them

The manuals are generated, not hand-written, so they can be refreshed when the app changes.

```bash
cd docs/manuals/src
npm install pdf-lib
node run.mjs          # writes the six PDFs into ../
```

- `build.mjs` — the rendering engine (cover, headings, numbered steps, field tables, notes, screenshots).
  Text is rendered with the standard PDF fonts, which only cover Latin-1, so `sanitize()` maps symbols
  (em dash, rupee sign, bullets, emoji) to safe equivalents. Add new mappings there rather than using
  exotic characters in the content files.
- `admin.mjs`, `staff.mjs`, `zone.mjs`, `center.mjs`, `finance.mjs`, `student.mjs` — the content of each
  manual as plain data: sections of `p`, `h2`, `steps`, `bullets`, `fields`, `note`, `shot`.
- `shots/` — the screenshots. Captured from the app running locally at a 980x820 viewport, which is just
  wide enough to keep the side menu docked (it collapses below 900px) while staying legible.

## Keeping them accurate

The field lists were taken from the live DOM, including fields hidden behind wizard tabs, so they match
the app exactly. If a form gains or loses a field, update the matching `fields:` block and re-run the
build. Bump `version` in the content file when you do.
