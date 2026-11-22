---
name: PiggyBank Android
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#3e4947'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#6e7977'
  outline-variant: '#bdc9c6'
  surface-tint: '#006a63'
  primary: '#005c55'
  on-primary: '#ffffff'
  primary-container: '#0f766e'
  on-primary-container: '#a3faef'
  inverse-primary: '#80d5cb'
  secondary: '#3b665f'
  on-secondary: '#ffffff'
  secondary-container: '#bdece2'
  on-secondary-container: '#416c65'
  tertiary: '#005f26'
  on-tertiary: '#ffffff'
  tertiary-container: '#007a33'
  on-tertiary-container: '#a1ffad'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#9cf2e8'
  primary-fixed-dim: '#80d5cb'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#00504a'
  secondary-fixed: '#bdece2'
  secondary-fixed-dim: '#a2d0c6'
  on-secondary-fixed: '#00201c'
  on-secondary-fixed-variant: '#224e47'
  tertiary-fixed: '#7ffc97'
  tertiary-fixed-dim: '#62df7d'
  on-tertiary-fixed: '#002109'
  on-tertiary-fixed-variant: '#005320'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 44px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system expresses a calm, disciplined, and approachable vision of personal finance for Android users. Rooted deeply in modern Material Design 3 and Jetpack Compose paradigms, it replaces traditional financial anxiety with visual clarity, intentional whitespace, and balanced tactility.

The brand personality balances institutional credibility with daily consumer warmth:
- **Trustworthy & Disciplined:** Financial ledgers and balances are presented with absolute geometric order and high contrast.
- **Empathetic & Approachable:** Softened geometry, refreshing mint accents, and forgiving touch targets mitigate financial stress.
- **Crisp & Modern:** A pristine off-white atmosphere layered with clean card containers, hairline borders, and fluid feedback states.

The visual style blends **Corporate Modern** with **Soft Minimalist Surface Architecture**, prioritizing tabular readability, frictionless thumb navigation, and clear visual hierarchy for budgeting, transactions, and savings goals.

## Colors

The color system delivers high-contrast legibility against daylight conditions while sustaining a calm, refreshing emotional tone.

### Palette Architecture
- **Primary (`#0F766E` Deep Emerald Teal):** Primary actions, active navigation icons, high-emphasis interactive surfaces, and key brand anchors.
- **Primary Container (`#CCFBF1` Soft Mint):** Active pill indicators, selected chips, and primary accent backings. Content on this tier uses `#0F4C5C`.
- **Canvas / Background (`#F8FAFC` Slate 50):** The base mobile viewport surface, creating soft separation behind floating white cards.
- **Surface (`#FFFFFF` Pure White):** Elevated cards, bottom sheets, navigation bars, and input fields.
- **Stroke / Outline (`#E2E8F0` Slate 200):** 1dp subtle structural borders separating cards and chrome without heavy shadows.
- **Text & Content:**
  - High Emphasis (`#0F172A` Deep Slate): Account totals, primary labels, headers.
  - Medium / Muted (`#64748B` Cool Slate): Supporting metadata, timestamps, input placeholders.
- **Semantic Feedback:**
  - Positive / Income: `#16A34A` text and glyphs on `#DCFCE7` background container.
  - Negative / Expense: `#DC2626` text and glyphs on `#FEE2E2` background container.

## Typography

The type scale is executed entirely in **Inter**, relying on systematic weights (`400`, `500`, `600`, `700`) to articulate hierarchy without visual noise.

### Numeric & Currency Rules
- All currency balances, transaction amounts, and quantitative indicators must enforce OpenType tabular figures (`font-feature-settings: "tnum" 1`) to guarantee perfect vertical alignment across ledger lists.
- Currency symbols use the static prefix `Rp` formatted at regular or medium weight preceding the bold tabular integer (e.g., `Rp 1.450.000`).
- Positive numbers prepend a `+` symbol in `#16A34A`; negative values prepend a `-` symbol in `#DC2626`.

## Layout & Spacing

The layout is built around the standard Android 360dp–412dp viewport baseline, adhering to an **8dp layout grid** (with a 4dp micro-step for compact icon and badge padding).

### Layout Rules
- **Canvas Margins:** Screen content maintains a constant `16dp` (`margin: 1rem`) horizontal margin on mobile screens, expanding to `24dp` on foldables and tablets.
- **Vertical Rhythm:** Content cards within list feeds and dashboards separate with `12dp` to `16dp` gaps (`space-md`).
- **Touch Targets:** All interactive icons, list rows, navigation items, and buttons strictly adhere to the Material 48dp minimum touch target bounding box, regardless of visible asset size.
- **Navigation Inset:** Canvas scrolling views must include a bottom content inset of at least `96dp` to prevent floating action buttons or the 80dp bottom navigation bar from obscuring content.

## Elevation & Depth

Depth in this design system is conveyed through **tonal separation coupled with hairline outlines**, avoiding deep, muddy drop shadows.

### Elevation Hierarchy
- **Level 0 (Flat Canvas):** `#F8FAFC` base screen layer. No elevation, no border.
- **Level 1 (Card & Container Surface):** `#FFFFFF` surface accompanied by a crisp `1dp` border in `#E2E8F0` and an ultra-subtle ambient drop shadow:
  - `box-shadow: 0px 1px 3px rgba(15, 23, 42, 0.04), 0px 1px 2px rgba(15, 23, 42, 0.02)` (approx. 1dp–2dp Android elevation).
- **Level 2 (Dropdowns, Dialogs & Bottom Sheets):** `#FFFFFF` surface with elevated ambient shadow:
  - `box-shadow: 0px 4px 16px -2px rgba(15, 23, 42, 0.08), 0px 2px 6px -1px rgba(15, 23, 42, 0.04)`.
- **Chrome Anchors (Navigation Bar & Top App Bar):** `#FFFFFF` surfaces with a direct `1dp` solid separation border (`#E2E8F0`) along the dividing edge.

## Shapes

The geometric scale reinforces friendly accessibility while maintaining structural discipline across standard Android components:

- **Cards & Surfaces (`16dp` / `1rem`):** Main metric cards, account ledger cards, and modal dialog sheets.
- **Controls & Form Elements (`12dp` / `0.75rem`):** Filled buttons, outlined text fields, and segmented control containers.
- **Chips, Badges & Nav Indicators (`9999px` Pill):** Transaction category tags, income/expense pill badges, and active navigation indicator capsules.
- **Micro Elements (`4dp` - `8dp`):** Progress bars, tooltip pointers, and inline checkboxes.

## Components

### Buttons
- **Primary Filled Button:** Height `48dp`, corner radius `12dp`. Background `#0F766E`, text and icon `#FFFFFF` (`label-lg`). Pressed state overlays a 12% white ripple. Disabled state maps to `#E2E8F0` background with `#94A3B8` text.
- **Tonal Button:** Height `48dp`, corner radius `12dp`. Background `#CCFBF1`, text `#0F4C5C`.
- **Outlined / Text Button:** Height `48dp`, border `1dp` solid `#E2E8F0`, text `#0F766E`.

### Cards & Ledger Items
- **Account Summary Card:** Background `#FFFFFF`, corner radius `16dp`, border `1dp` `#E2E8F0`. Internal padding `16dp` or `20dp`. Uses Level 1 elevation.
- **Transaction Row:** Height `64dp` to `72dp`. Left slot: 40dp rounded circle containing category icon tinted with soft semantic background. Middle slot: title (`title-md`) and category/date (`body-sm`). Right slot: tabular balance formatted in `title-md` using semantic positive/negative text colors.

### Chips & Badges
- **Status Badges:** Height `24dp` to `28dp`, full pill radius (`9999px`), horizontal padding `10dp`.
  - Income: `#DCFCE7` background, `#16A34A` label.
  - Expense: `#FEE2E2` background, `#DC2626` label.
- **Filter Chips:** Height `32dp`, pill radius, border `1dp` `#E2E8F0`, background `#FFFFFF`. When selected: border-free `#CCFBF1` background with `#0F4C5C` label.

### Form Inputs
- **Text & Currency Field:** Container height `56dp`, background `#FFFFFF`, corner radius `12dp`, border `1dp` `#E2E8F0`. Active focus shifts border to `2dp` `#0F766E`.
- **Prefix / Suffix:** Static currency text `Rp` anchored in `#64748B` with `title-md`.

### Navigation Bar (Material 3)
- **Container:** Height `80dp`, background `#FFFFFF`, border-top `1dp` `#E2E8F0`.
- **Tabs (3 Primary Destinations):**
  1. *Kas* (Wallet)
  2. *Celengan* (Piggy Bank / Savings)
  3. *Profil* (Account)
- **Active State:** Active item sits within a `64dp × 32dp` pill indicator in Soft Mint (`#CCFBF1`). Icon and destination label colored in Deep Emerald Teal (`#0F766E`).
- **Inactive State:** Icon and label colored in Cool Slate (`#64748B`).