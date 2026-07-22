# UX Foundation: Shams.ma
**PRD Reference**: docs/prd-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: UX Designer

## 1. User Personas (minimal — one per role)
| Persona | Role | Goal | Pain Point |
|---|---|---|---|
| Amina | Homeowner | Find a trustworthy installer and know her real ROI before spending anything | Doesn't know who to trust; ROI claims from installers feel like sales pitches |
| Youssef | Installer | Get qualified leads in his coverage area without chasing cold contacts | No unified channel; spends time on leads outside his service area |
| Admin (ops team) | Admin | Keep the marketplace trustworthy by verifying installer credentials | Manual process today (email/paper); needs a queue, not a spreadsheet |

## 2. Information Architecture / Site Map
```
[App Root] (role-based routing after login)
├── Public
│   ├── Landing / Home
│   ├── ROI Calculator (no login required)
│   └── Login / Register
├── Homeowner (/homeowner)
│   ├── Browse Installers (filtered to coverage area)
│   ├── My Quote Requests (list + detail)
│   ├── Booking + Payment
│   └── Profile
├── Installer (/installer)
│   ├── Lead Inbox (quote requests)
│   ├── Coverage Zone Settings
│   ├── Certification Upload / Status
│   └── Profile
└── Admin (/admin)
    ├── Certification Review Queue
    ├── Installer Directory
    └── Booking / Payment Overview
```

## 3. Core User Flows (top 3 journeys)

### Flow 1: Homeowner — ROI Calculation → Booking
```
(Landing) → [Enter address + energy usage] → [ROI/payback estimate shown]
    → [Browse verified installers in coverage area] → <Any installers found?>
        ↓ No                                              ↓ Yes
   [Empty state: "No verified installers in your area yet"]  [Select 1+ installers → Request quote]
                                                                  → [Wait for installer response(s)]
                                                                  → [Compare quotes] → [Select one → Book]
                                                                  → [Pay deposit via CMI] → <Payment succeeds?>
                                                                       ↓ No                    ↓ Yes
                                                                  [Payment failed, retry]   (Booking confirmed)
```

### Flow 2: Installer — Registration → Verified → Receiving Leads
```
(Register as Installer) → [Fill business profile] → [Set coverage zone (map pin + radius)]
    → [Upload certification document(s)] → [Status: Pending review]
    → <Admin approves?>
        ↓ No                                  ↓ Yes
   [Status: Rejected + reason, resubmit]   [Status: Approved — now discoverable]
                                                → [Lead Inbox receives quote requests]
                                                → [Respond with quote or decline] → (Homeowner sees response)
```

### Flow 3: Admin — Certification Review
```
(Admin Dashboard) → [Certification Review Queue, sorted oldest-first]
    → [Open a submission] → [View document + installer profile] → <Meets requirements?>
        ↓ No                                                         ↓ Yes
   [Reject + reason note] → (Installer notified)                [Approve] → (Installer notified, now listed)
    → [Audit log entry recorded automatically]
```

## 4. Key Screen Wireframes (text-based)

### Screen: ROI Calculator (public)
```
┌─────────────────────────────────────┐
│ Shams.ma            [Login/Register]│
├─────────────────────────────────────┤
│  Estimate your solar savings         │
│  [Address input.....................]│
│  [Monthly energy bill (MAD)....]     │
│  [Roof size (m²) — optional....]     │
│           [Calculate ROI]            │
├─────────────────────────────────────┤
│  Estimated payback: ~7.2 years       │
│  Estimated annual savings: 12,400 MAD│
│       [See verified installers →]    │
└─────────────────────────────────────┘
```

### Screen: Installer Lead Inbox
```
┌─────────────────────────────────────┐
│ Shams.ma — Installer   [Youssef ▾]   │
├─────────────────────────────────────┤
│ Leads  [Requested] [Quoted] [Booked] │
├─────────────────────────────────────┤
│ Amina B. — Casablanca                │
│  "Requested 2026-07-20"  [Respond →] │
├─────────────────────────────────────┤
│ (empty state if none:)               │
│  "No leads yet. Make sure your       │
│   coverage zone is set."             │
└─────────────────────────────────────┘
```

### Screen: Admin Certification Review Queue
```
┌─────────────────────────────────────┐
│ Shams.ma — Admin                     │
├─────────────────────────────────────┤
│ Pending Certifications (3)           │
├─────────────────────────────────────┤
│ Youssef Solar Co.  [View] [Approve]  │
│                            [Reject]  │
├─────────────────────────────────────┤
│ (empty state:) "No pending reviews." │
└─────────────────────────────────────┘
```

## 5. Screen States
| Screen | Empty State | Loading | Error | Success |
|---|---|---|---|---|
| Browse Installers | "No verified installers in your area yet — check back soon" | Skeleton cards | "Couldn't load installers, retry" | List of installer cards with coverage badge |
| Lead Inbox | "No leads yet. Make sure your coverage zone is set." | Skeleton rows | "Couldn't load leads, retry" | Table of quote requests by status |
| Certification Review Queue | "No pending reviews." | Skeleton rows | "Couldn't load queue, retry" | List sorted oldest-first |
| Booking + Payment | N/A (always has a quote context) | "Processing payment..." spinner (blocks double-submit) | "Payment failed — [Retry] or [Choose different installer]" | "Booking confirmed" + confirmation email notice |
