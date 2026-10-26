---
name: Cupertino Native Assistant
colors:
  surface: '#faf8fd'
  surface-dim: '#dbd9de'
  surface-bright: '#faf8fd'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f7'
  surface-container: '#efedf2'
  surface-container-high: '#e9e7ec'
  surface-container-highest: '#e3e2e6'
  on-surface: '#1b1b1f'
  on-surface-variant: '#414755'
  inverse-surface: '#2f3034'
  inverse-on-surface: '#f2f0f4'
  outline: '#717786'
  outline-variant: '#c1c6d7'
  surface-tint: '#005bc1'
  primary: '#0058bc'
  on-primary: '#ffffff'
  primary-container: '#0070eb'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#4c4aca'
  on-secondary: '#ffffff'
  secondary-container: '#6664e4'
  on-secondary-container: '#fffbff'
  tertiary: '#006b27'
  on-tertiary: '#ffffff'
  tertiary-container: '#008733'
  on-tertiary-container: '#f7fff2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004493'
  secondary-fixed: '#e2dfff'
  secondary-fixed-dim: '#c2c1ff'
  on-secondary-fixed: '#0c006a'
  on-secondary-fixed-variant: '#3631b4'
  tertiary-fixed: '#72fe88'
  tertiary-fixed-dim: '#53e16f'
  on-tertiary-fixed: '#002107'
  on-tertiary-fixed-variant: '#00531c'
  background: '#faf8fd'
  on-background: '#1b1b1f'
  surface-variant: '#e3e2e6'
typography:
  large-title:
    fontFamily: Inter
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 41px
    letterSpacing: 0.37px
  title-1:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: 0.36px
  title-2:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.26px
  headline:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: -0.41px
  body:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: -0.41px
  callout:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 21px
    letterSpacing: -0.32px
  subhead:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: -0.24px
  footnote:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: -0.08px
  caption-1:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0px
  caption-2:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 13px
    letterSpacing: 0.07px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.25rem
  space-xl: 1.5rem
---

## Brand & Style
The brand personality embodies the poise, rigor, and understated intelligence of a first-party iOS utility. Designed exclusively for university students, it delivers calm clarity amidst academic information overload. The emotional response must be reassuring, orderly, and frictionless—giving the impression that the operating system itself natively parsed academic obligations.

The design movement strictly adheres to modern iOS Human Interface Guidelines:
- **Clean Precision:** Heavy reliance on iOS system grouping, high-density structured lists, and generous safe margin whitespace.
- **Translucency & Layering:** System materials including dynamic background blurs across toolbars and navigation bars.
- **Native Polish:** Continuous corner curves (squircles), subtle 0.5pt hairline dividers, and fluid kinetic feedback.

## Colors
The palette leverages standard iOS system semantics, anchored by an authoritative single accent and high-utility category signifiers.

### Canvas & Neutral Surfaces
- **System Grouped Background:** `#F2F2F7` forms the primary canvas for grouped tables and views.
- **Card & Surface Background:** Pure `#FFFFFF` elevates content above the grouped background.
- **Primary Label:** `#000000` for critical headlines and primary text layers.
- **Secondary Label:** `#6C6C70` for metadata, timestamps, and supporting body copy.
- **Separators & Hairlines:** `#C6C6C8` applied at 0.5px thickness.

### Interactive Accent
- **iOS System Blue:** `#007AFF` governs unread state indicators, key calls-to-action, text links, active tab bar items, and interactive highlights.

### Academic Event Badges (Category Tile Pigments)
Applied strictly to 40px squircle category tiles and contextual tags:
- **Class Cancellation:** `#FF3B30` (System Red)
- **Quizzes & Examinations:** `#AF52DE` (System Purple)
- **Deadlines & Cutoffs:** `#FF9500` (System Orange)
- **Results & Grades:** `#34C759` (System Green)
- **Tuition & Financial Services:** `#30B0C7` (System Teal)
- **Career & Opportunities:** `#5856D6` (System Indigo)
- **Campus Life & Events:** `#FF2D55` (System Pink)
- **Administrative Notices:** `#8E8E93` (System Gray)

## Typography
The system adopts an Apple-native hierarchy utilizing Inter. Tracking, line spacing, and weights correlate directly with iOS Human Interface dynamic type standards.

- **Large Title:** Reserved exclusively for collapsed or top-level navigation states (e.g., "Inbox", "Digest").
- **Headline vs. Body:** Both share a 17pt foundation to preserve baseline alignment in dense email rows; differentiate importance purely via 600 weight versus 400 weight.
- **Footnote & Caption:** Used for academic sender timestamps, room allocations, course codes, and badge sub-labels. Maintain high contrast against pure white card surfaces.

## Layout & Spacing
The layout adheres strictly to an 8pt architectural grid mapped across a 393x852 viewport.

- **Safe Boundaries & Margin:** Standard horizontal screen inset is 20px (`margin: 1.25rem`), aligning cards and title groups to iOS native margins.
- **Touch Ergonomics:** All interactive touch targets (e.g., list rows, trailing action icons, search fields, chips) strictly enforce a minimum dimension of 44x44pt.
- **Card Containers:** Inset grouped content areas run full width minus margin (`calc(100vw - 40px)`). 
- **Internal Cell Spacing:** Vertical row rhythm inside cards utilizes 12px or 16px padding, with an 8px spacing standard between metadata lines.

## Elevation & Depth
Elevation mimics iOS layered materials rather than high-contrast drop shadows.

- **Surface Tiers:**
  - **Level 0 (Base Canvas):** Background `#F2F2F7`.
  - **Level 1 (Inset Grouped Cards):** `#FFFFFF` with an ambient, diffused shadow: `0px 8px 24px rgba(0, 0, 0, 0.06)`.
  - **Level 2 (Modals & Action Sheets):** `#FFFFFF` with ambient separation: `0px 12px 32px rgba(0, 0, 0, 0.12)`.
- **System Materials & Blur:**
  - **Navigation & Tab Bar:** Backed by an ultra-thin Apple material: `rgba(255, 255, 255, 0.78)` overlaid with a `backdrop-filter: blur(20px) saturate(180%)`.
- **Separators:** Inset 0.5px hairline dividers (`#C6C6C8`) applied between list cells; must not extend beneath left-aligned 40px category icons.

## Shapes
Shapes employ continuous-curve iOS squircle geometry (`corner-smoothing: 60%`).

- **Inset Grouped Parent Cards:** 16px to 20px squircle radius depending on section hierarchy.
- **Interactive Buttons:** 12px squircle radius for prominent actions; standard circular rounding for accessory icon taps.
- **Category Icon Tiles:** Exactly 40x40px with a 10px squircle radius.
- **Segmented Controls & Filter Chips:** Fully rounded capsule/pill shapes (9999px) or standard 8px segmented buttons conforming to native segmented controls.

## Components

### Inset Grouped Email & Alert Cards
- **Container:** `#FFFFFF` background, 16px radius squircle, ambient shadow (`0 8px 24px rgba(0,0,0,0.06)`).
- **Row Architecture:**
  - **Leading Element:** 40x40px category squircle tile with a high-contrast white glyph, centered vertically.
  - **Unread Marker:** 8x8px `#007AFF` circle placed next to timestamp or name.
  - **Middle Stack:** Senders displayed in `Headline` (17pt/600), email subject in `Body` (17pt/400), two-line preview in `Subhead` (15pt/400, `#6C6C70`).
  - **Trailing:** Timestamp displayed in `Footnote` (13pt, `#6C6C70`) accompanied by a subtle right disclosure chevron (`#C6C6C8`).
- **Dividers:** 0.5px hairline (`#C6C6C8`) inset 68px from the left edge to align precisely with text start.

### Buttons & Quick Actions
- **Primary Action:** Solid `#007AFF` background, pure `#FFFFFF` label (`Headline` 17pt/600), minimum height of 50px, 12px squircle radius.
- **Secondary Action:** Tinted `#007AFF` at 10% opacity with `#007AFF` text.
- **Destructive Action:** `#FF3B30` text or tinted background for discarding academic notifications.

### Category Icon Squircle Tiles
- Dimensions fixed at 40x40px with 10px continuous corners.
- Background uses the specified 8 categorical hues (Red, Purple, Orange, Green, Teal, Indigo, Pink, Gray).
- Houses centered Apple SF Symbol or standard glyphs in `#FFFFFF` at 20px optical size.

### Filter Chips & Segmented Controls
- **Segmented Control:** Native style mounted on `#E5E5EA` backing, active item elevated on `#FFFFFF` squircle with `0 2px 4px rgba(0,0,0,0.08)`.
- **Category Chips:** Horizontal scrolling collection, 32px height, 16px pill border radius, 0.5px hairline outline when unselected, solid `#007AFF` tint when filtered.

### Input Fields & Search Bars
- **iOS Search Field:** 36px height, rounded 10px squircle, `#E3E3E8` solid fill, `#8E8E93` magnifying glass and placeholder text.
- **Form Inputs:** 44px minimum height, inset grouped list structure, white fill with inline labels.

### Tab Bar
- Native 83px bottom bar including 34px Home Indicator clearance.
- Frosted glass material (`backdrop-filter: blur(20px)`).
- Active item uses `#007AFF`; inactive items use `#8E8E93`.
- 0.5px hairline top border `#C6C6C8`.