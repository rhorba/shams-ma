# UI Foundation: Shams.ma
**UX Reference**: docs/ux-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: UI Designer

## 1. Design Approach
- **Strategy**: UI framework — MUI (Material UI) themed with brand tokens, not a custom design system
- **Rationale**: Three role-based dashboards (homeowner, installer, admin) are form- and table-heavy — MUI's DataGrid, form, and layout primitives cover this out of the box. Building a custom component library for an MVP marketplace is YAGNI. Theme customization gives brand identity without the cost of custom components.

## 2. Design Tokens (MUI theme overrides)
```css
/* Colors — solar/renewable brand identity */
--color-primary:     #F2A93B;  /* solar gold — primary CTAs, active states */
--color-primary-dark:#C6821F;  /* hover state */
--color-secondary:   #1E7A4C;  /* renewable green — secondary actions, "verified" badge */
--color-background:  #FFFFFF;  /* dark mode: #0F0F0F */
--color-surface:     #F7F5F2;  /* dark mode: #1A1A1A */
--color-error:       #DC2626;
--color-warning:     #F59E0B;
--color-success:     #16A34A;
--color-text:        #1A1A1A;  /* dark mode: #F0F0F0 */
--color-text-muted:  #666666;  /* dark mode: #999999 */
--color-border:      #E0E0E0;  /* dark mode: #333333 */

/* Typography */
--font-family:   'Inter', system-ui, sans-serif;
--font-size-sm:  14px;
--font-size-md:  16px;
--font-size-lg:  20px;
--font-size-xl:  24px;

/* Spacing scale (4px base grid, MUI default spacing(n) = 8px * n works fine) */
--spacing-xs: 4px;  --spacing-sm: 8px;
--spacing-md: 16px; --spacing-lg: 24px;
--spacing-xl: 32px;
```

**Contrast check**: primary gold (#F2A93B) on white fails AA for body text (used for CTAs/backgrounds only, never small text-on-white); all body/label text uses `--color-text` (#1A1A1A) on white/surface — passes AA (4.5:1+). "Verified" badge uses green (#1E7A4C) text/icon + a checkmark icon, never color alone.

## 3. Component Inventory
| Component | Reuse Existing (MUI) | Build New | Notes |
|---|---|---|---|
| Button | `Button` | No | primary (solar gold), secondary (outlined green), danger (red) variants via theme |
| Text/Number Input | `TextField` | No | always-visible labels per UX spec |
| Data table (lead inbox, cert queue, booking overview) | `DataGrid` | No | sortable, empty-state slot used for UX empty states |
| Status Badge (Pending/Approved/Rejected, Requested/Quoted/Booked) | — | Yes (thin wrapper over `Chip`) | color + icon, never color alone (accessibility) |
| Map picker (coverage zone: pin + radius) | — | Yes | wraps a lightweight map lib (e.g., Leaflet) — no existing MUI equivalent |
| File upload (certification docs) | — | Yes (thin wrapper over `Button` + hidden input) | drag-drop optional, MVP: click-to-upload sufficient |
| Card (installer browse listing) | `Card` | No | per UX wireframe: coverage badge → business name → verified badge → CTA |
| Modal/Dialog (confirm booking, reject cert w/ reason) | `Dialog` | No | |
| Toast/Snackbar (success/error feedback) | `Snackbar` | No | |

## 4. Responsive Breakpoints
| Breakpoint | Width | Layout Notes |
|---|---|---|
| Mobile | < 768px | Single column; DataGrids become card-list layout (per UX table rule); bottom-anchored primary CTA on booking/payment screens |
| Tablet | 768–1024px | 2-column where applicable (e.g., installer browse grid) |
| Desktop | > 1024px | Full DataGrid tables, 3-column installer browse grid, max content width 1280px centered |

## 5. Accessibility Baseline
- Color contrast: AA minimum (4.5:1 body text, 3:1 large text/icons) — verified above for token set
- Focus indicators: MUI's default focus ring retained (not stripped via custom CSS)
- Semantic HTML first: forms use real `<label>`/`<input>` via MUI TextField (already semantic); ARIA only for custom components (map picker, status badges) where native semantics don't apply
- Status conveyed via icon + text + color together (never color alone) — applies to verification status and quote/booking status badges
