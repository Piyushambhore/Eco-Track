# EcoTrack — UI Build Brief

**App name:** EcoTrack
**Tagline:** Turn Waste Into Worth
**Vision:** An AI-powered waste management and recycling ecosystem connecting citizens, waste collectors, scrap dealers, and recycling organisations through one unified platform.

You are building a **working, clickable UI prototype** of this app. Follow this file exactly. Do not change names, copy, colours, radii, spacing, or structure. Do not ask questions before building. Build everything below in one pass and make assumptions where something is unclear, stating them in one line at the end.

---

## 1. Deliverable

- **One self-contained HTML file** (HTML + CSS + JS inline, no build step, no frameworks required).
- It shows **6 core mobile screens as one connected flow, in 2 rows of 3 phones**, each in an iPhone 15 Pro-style frame.
- The prototype must **actually work**: buttons, tabs, chips, forms, sheets, and the photo flow respond (see section 8).
- Light mode only. Keep `color-scheme: light`.
- Production-quality look. Each screen should look like a real shipped app, not a wireframe.

### Hard technical constraints (if this is hosted or published as a web page)
- External scripts only from `cdnjs.cloudflare.com`, `cdn.jsdelivr.net/npm/`, `cdn.tailwindcss.com`, `code.jquery.com`. External CSS only from `fonts.googleapis.com` (fonts from `fonts.gstatic.com`). Pin exact versions. Everything else is blocked.
- **No remote images.** Use embedded data URIs, CSS, or inline SVG. If you cannot use real photographs, use neutral, muted photo placeholders (soft natural-tone gradients with a small caption such as "Photo placeholder") and say so in your final note. Never use colour overlays or heavy tints on photos.
- No network calls to other sites. No API keys. No `<form>` tags needed; use button handlers.
- Do not rely on `localStorage` for anything essential. Wrap any storage use in try/catch.
- Include `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">`. Respect safe-area insets on the page edges.
- File size under 16 MB. Keep it practical, ideally under 100 KB.

---

## 2. Device and layout

- **Device:** iPhone 15 Pro, **393 × 852** px per screen.
- **Status bar:** time **9:41**, signal bars and battery on the right, dynamic island pill (about 120 × 34) centred at the top.
- **Frame:** dark (`#171A16`) 7px bezel via box-shadow, 48px corner radius, soft outer shadow. Identical size and chrome on all six phones.
- **Overview layout:** CSS grid, 3 columns × 2 rows on wide screens (≥ 1380 px). Above each phone a small label with the screen number and name (for example "1. Home Dashboard"). The numbering is legitimate because it is a flow.
- **Responsive scaling:** scale the phones with a CSS variable `--s` (transform scale, origin top left, wrapper sized `393*s × 852*s`). Use 3 columns ≥ 1380 px, 2 columns ≥ 950 px, 1 column below. Recompute on resize. Scale never exceeds 1.
- Page background behind the phones: a slightly darker neutral than the app background (for example `#E9EBE3`). Header above the grid: "EcoTrack" (28px, 600) and one line saying the screens are one connected flow and all data is demo data.

---

## 3. Design system

### Philosophy
Premium, modern, clean, trustworthy, environmentally conscious, and practical. It must feel like a professionally designed Indian climate-tech startup product, not a generic AI-generated dashboard and not a childish recycling app.

### Font
**Inter** (load from Google Fonts: weights 400, 500, 600) with fallback stack `system-ui, -apple-system, "Segoe UI", sans-serif`.
- Headings: 600
- Body: 400
- Labels: 500
- Sentence case everywhere. No tracked-out all-caps eyebrow labels. No spaced em-dash labels.

### Colours (use these tokens exactly)
| Token | Hex |
|---|---|
| background | `#F5F6F0` |
| card | `#FFFFFF` |
| text | `#171A16` |
| muted | `#777D74` |
| brand (lime) | `#B9E769` |
| brand_dark (forest) | `#263D24` |
| border | `#E5E8DE` |
| success | `#3C7950` |
| warning | `#B98532` |

Derived tints allowed (keep subtle): lime tint `#E8F5CC` for the scan card and info cards, lime tint border `#D6E8AE`, success badge background `#E3F0E7`, warning badge background `#F6EBD6` with text `#8A5F18`, warning card `#FBF4E4` with border `#EBD9B0`, neutral badge `#EEF0E8`.

### Brand usage
Lime is the primary accent for active controls, primary actions, rewards, and key environmental indicators. Deep forest green is used sparingly for strong contrast. Black and dark green establish hierarchy. **Avoid large bright-green backgrounds.** Warning and success colours appear only where their meaning requires them.

### Radii
- Large cards: **24px**
- Standard cards: **18px**
- Photo covers: **14px**
- Buttons: **pill (999px)**
- Input fields: **14px**

### Spacing
- **18px** horizontal screen padding
- **8px** internal spacing increments
- **20–24px** between major sections (compact to 12–16px only where a screen would overflow)

### Imagery
Realistic, high-quality, natural photography of recyclable materials, clean collection points, recycling centres, and Indian urban neighbourhoods. No colour overlays, artificial tints, or excess saturation. (If real photos are not possible, see the placeholder rule in section 1.)

### Icons
One consistent set: **Lucide-style minimalist outline icons**, 2px rounded strokes, `currentColor`. Do not mix icon styles.

### Buttons
- **Primary:** lime background, dark text (`#263D24`), label weight 600, pill, **min height 48px**.
- **Dark primary (hero action on the scan card only):** forest background, white text.
- **Secondary:** white background, 1px border `#E5E8DE`, dark label, pill.
- **Destructive:** restrained semantic styling, no loud red fills.

### Cards
White surface on the off-white background, 1px `#E5E8DE` border, very subtle shadow (`0 1px 2px rgba(23,26,22,.04)`), generous padding, consistent radii. No heavy shadows, no gradient washes.

### Bottom navigation
Floating white pill (inset 18px each side, 22px from bottom, 64px tall, fully rounded, 1px border, soft shadow) with **5 destinations: Home, Explore, Scan, Activity, Profile**.
- The central **Scan** action is a **54px lime circle**, raised about 16px, with a white ring.
- Active tab uses a **lime circular background (44px) with a dark icon**.
- Each button has an `aria-label`; the active one is labelled "(current)".
- The active tab must match the screen.

### Microinteractions
Subtle button feedback, upload/preparing progress, classification status transitions, collection status updates, reward confirmation. Restrained and functional. Respect `prefers-reduced-motion`.

### Accessibility
Readable text, adequate contrast, touch targets at least 40–48px, accessible labels, clear status indicators. **Never communicate status by colour alone**: every status badge has an icon and a text label.

---

## 4. Brand identity

**Personality:** Helpful, Trustworthy, Intelligent, Community-driven, Sustainable, Practical.
**Visual direction:** A premium Indian climate-tech startup that combines the simplicity of a consumer app with the reliability of a real-world service platform.
**Logo concept:** A minimal circular leaf combined with a subtle recycling-loop motif, simple enough to work as an app icon. Build it as inline SVG (a dark-green circle with a lime leaf and a thin loop arc). Use it in the Home header next to the wordmark "EcoTrack" (17px, 600).

**Avoid:** cartoon leaves everywhere, excessive gradients, neon green backgrounds, generic AI dashboard layouts, too many charts, unnecessary glassmorphism, excessive rounded floating elements, fake environmental statistics, fake verified badges, unrealistic maps, overcrowded screens.

---

## 5. Screens (build all six, with all content listed)

Screens must fit inside 852px without clipping. If a screen has more content than fits, **do not drop content**: make the body area vertically scrollable inside the phone (hidden scrollbar, content ends clear of the nav), or move secondary content into a bottom sheet that opens from a button. Content above the fold must show the hierarchy described for each screen.

Bottom nav appears on: Screen 1 (Home active), Screen 4 (Explore active), Screen 5 (Explore active), Screen 6 (Activity active). Screens 2 and 3 are the focused capture/result flow and use a header with a back button instead (Scan is the context there).

### Screen 1. Home Dashboard
**Purpose:** Introduce EcoTrack and provide immediate access to waste identification, nearby services, and environmental activity.

**Content (top to bottom):**
1. Header: EcoTrack logo left; location selector in the middle showing **"Kolhapur, Maharashtra"** (sample demo content) with a pin icon and chevron; circular profile avatar right.
2. Greeting: **"Make a little difference today."**
3. Supporting text: **"Let's give your waste a better destination."**
4. **Large lime-tinted scan card** (24px radius): camera icon in a dark circle, title **"What are you throwing away?"**, supporting text **"Identify waste and find the right way to dispose of it."**, prominent dark button **"Scan your waste"**.
5. Section **"Your impact"**: a white card with three metrics: **"12 kg" materials recovered**, **"8" actions completed**, **"5" collection requests**. Add a visible **"Demo data"** badge.
6. Section **"Nearby services"** with a **"See all"** action. Horizontal service cards with a photo, provider name, accepted materials, approximate distance, and a restrained verification badge only for demo providers that are meant to be shown as verified. Example categories: Scrap dealers, Recycling centres, E-waste collection. Use fictional demo names (for example "GreenCycle Recycling", "E-Waste Collect", "Metro Scrap Traders"). Never use names of real businesses.
7. Section **"Continue your impact"**: a recent waste report (for example "Plastic bottles report") with a status badge ("Request submitted").
8. Bottom nav: Home active.

**Hierarchy:** the scan card dominates; stats are secondary; nearby services are the next step.
**Interactions:** Scan opens Screen 2. Location selector opens a sheet to change area (Kolhapur, Sangli, Pune, Mumbai, all in Maharashtra) and updates the label and the default pickup location. "See all" and service cards open Screen 4. Impact metrics open Screen 6. The report card opens Screen 3.

### Screen 2. AI Waste Scanner
**Purpose:** Let a user photograph waste and receive an understandable classification and disposal recommendation.

**Content:**
1. Header: circular back button, title **"Identify waste"**, small help icon button.
2. Progress: **"Step 1 of 3"** with a 3-segment bar.
3. Large camera capture area (24px radius) with a realistic viewfinder look and **subtle white corner markers**.
4. Instruction: **"Take a clear photo of the item or waste pile."**
5. Supporting text: **"For mixed waste, photograph the pile from a safe distance."**
6. Primary lime button **"Take a photo"**.
7. Secondary white button **"Upload from gallery"**.
8. Small privacy note: the photo is used only to identify waste and recommend suitable disposal options.
9. Compact safety note (warning style): **"Never touch unknown chemicals, sharp objects, or potentially hazardous waste to take a photo."**
10. Category chips (selectable): Plastic, Paper, Metal, Glass, E-waste, Organic, Mixed waste (horizontally scrollable).

**Interaction notes:**
- Request camera permission only when the user initiates capture (use a file input with `capture="environment"` for "Take a photo"; plain file input for gallery).
- Location permission is requested separately, only when the user asks to find nearby services.
- Show progress ("Preparing photo…") and recoverable errors (for example "Choose an image file.").
- After the photo is chosen, update the step to **"Step 2 of 3"** and go to Screen 3.
- **Do not fabricate an AI result.** See section 7.

### Screen 3. Waste Analysis & Disposal
**Purpose:** Show the classification result and connect the user with a suitable disposal, collection, or recycling action.

**Content:**
1. Header: back button, title **"Waste analysis"**, share icon.
2. Large rounded photo of the submitted waste (the user's photo once chosen).
3. Result card: heading (for example **"Plastic bottles"**), category label **"Dry recyclable waste"**, and a confidence indicator **only when a real model result supports it** (in this prototype use "AI classification not connected").
4. Short explanation: **"Empty the bottles and keep them separate from food waste. Local recycling options may differ."**
5. A **"Not right? Change category"** link that lets the user correct the category.
6. Section **"Recommended next step"** with a prominent lime button **"Find nearby recyclers"**.
7. Secondary action **"Request a collection"**.
8. Smaller action **"Save this report"**.
9. Section **"Accepted by nearby services"**: provider cards with distance, accepted materials, estimated availability when known, and verification status. Show only providers that accept the selected category. If none do, show a clear empty state.
10. A restrained warning card if the item may need specialist handling or classification is uncertain (for example E-waste: "Needs specialist collection").
11. Bottom information note: **"AI classification is an estimate. Final acceptance depends on the service provider."**

**Interaction notes:** use the category together with local disposal rules; never recommend a provider only because it is close; only show availability when current information supports it; for hazardous or unidentified materials, prioritise safe guidance and authorised services; collection requests are either a real backend workflow or explicitly marked as simulated.

### Screen 4. Nearby Services & Collection
**Purpose:** Help users find a suitable service and submit a collection request.

**Content:**
1. Header: back button, title **"Nearby services"**, filter icon.
2. Search field: **"Search recyclers or collection points"** (14px radius).
3. Filter chips: **All, Scrap dealers, Recycling centres, E-waste, Collection services**.
4. Large map section: realistic-looking styled map (inline SVG: roads, a park, a river), user's approximate location dot, 3 service markers, zoom controls, and a **Map / List** toggle. Label it **"Illustrative map"**.
5. Service list below the map: white cards with photo, provider name, category, distance, accepted materials, operating status **only when known** (otherwise "Hours not confirmed"), and a verification indicator **only when confirmed**.
6. Example card: **"GreenCycle Recycling"**, **"Accepts paper, cardboard and selected plastics"**, **"1.8 km away"**, marked **"Demo provider"**.
7. **Provider detail sheet** (opens when a card is tapped): accepted materials, service area, collection options, and a contact/request action.
8. **Collection request form** (bottom sheet): waste category, approximate quantity, pickup location, preferred time, optional notes, primary lime button **"Submit collection request"**.
9. **Confirmation state:** **"Request submitted"** with a **tracking reference** (format `ET-XXXXXX`) and a status badge.

**Interaction notes:** location access is permission-based with a manual location alternative; only recommend services that accept the selected category; do not show fabricated hours, availability, ratings, or live collector locations. Collection states: **Submitted, Accepted, Scheduled, Collected, Verified, Cancelled, Disputed** (show them as badges; the prototype only produces "Submitted").

### Screen 5. Scrap Marketplace
**Purpose:** Connect users who have recyclable materials with registered scrap dealers and buyers.

**Content:**
1. Header: title **"Recycle & earn"**, search icon, notification icon.
2. Supporting text: **"Give recyclable materials another life."**
3. Horizontal category chips (selectable, filter the feed): Paper, Plastic, Metal, Glass, E-waste.
4. Compact info card: **"Prices and acceptance depend on material quality, quantity, and local demand."**
5. Primary lime button **"List recyclable waste"**.
6. Marketplace feed of clean white cards with material photos. Each listing: material type, approximate quantity, collection area, listing status. Example: **"Cardboard boxes"**, **"Approx. 8 kg"**, **"Pickup area: sample location"**. Label sample listings as **demo data**.
7. Dealer cards: business name, supported materials, verification status when confirmed, quotation availability.
8. Listing details (sheet): material photos, quantity, condition, location, collection preferences, optional seller asking price.
9. Quotation section: registered dealers can submit an offer (simulated, clearly labelled).
10. Transaction states as badges: **Listed, Quoted, Accepted, Handover pending, Completed, Disputed**.
11. Small note: **"Final quantity and price must be confirmed by the buyer and seller."**

**Interaction notes:** only registered and verified dealers can quote; prevent conflicting acceptances of one listing; do not estimate exact weight from a photo; no guaranteed prices or instant payments; simulated transactions are clearly labelled.
**Visual direction:** a credible local trading platform, not an online shopping clone. Prioritise trust, material information, and straightforward actions.

### Screen 6. Impact & Rewards
**Purpose:** Encourage continued participation while transparently reporting verified environmental actions.

**Content:**
1. Header: title **"Your impact"**, profile/settings icon.
2. Greeting: **"Every verified action counts."**
3. Large white summary card with a **lime circular progress illustration** and **"8 verified actions"** (ring shows verified out of reported, labelled as sample).
4. Three compact stats: **"12 kg recovered"**, **"5 completed collections"**, **"120 points earned"**, clearly identified as illustrative sample data.
5. Section **"Your environmental contribution"**: verified quantities by category as simple horizontal bars for paper, plastic, metal, other (5, 4, 2, 1 kg to match the 12 kg total).
6. **Separate totals** for reported, collected, and confirmed recycled material.
7. Estimated emissions benefit shown separately with a clear **"Estimate"** badge and an info icon explaining the calculation method (documented emission factors applied to verified quantities).
8. Section **"Reward history"**: dated entries for completed and verified activities (for example "Cardboard handover confirmed, 4 Oct 2026, +30 pts").
9. **Leaderboard preview "Campus challenge"** with anonymised sample participants and sample rankings.
10. Achievement cards: **"First Verified Recycling"**, **"Community Contributor"**, **"Consistent Recycler"** (earned vs not yet earned must be distinguishable by icon and text, not colour alone).
11. Primary button **"Explore more ways to help"**.
12. Bottom nav: Activity active.

**Interaction notes:** reward points are calculated by the backend; no high-value points for merely uploading a photo; carbon calculations use documented factors and verified quantities; label sample numbers and simulated achievements; prevent duplicate rewards for the same verified event.
**Visual direction:** restrained and data-led. No exaggerated claims, meaningless rings, or excessive gamification.

---

## 6. Shared components (build once, reuse everywhere)

| Component | Spec |
|---|---|
| Header | Consistent title alignment, back-button style, icon sizes, horizontal padding on every screen. Height 48px. Round icon buttons 40px, white, 1px border. |
| Primary button | Lime, dark text, bold label, pill, min 48px. |
| Secondary button | White, subtle border, dark label, pill. |
| Service card | White rounded card with provider identity, accepted materials, distance, status. |
| Status badge | Small, icon plus text. Never colour alone. Variants: neutral, success, warning. |
| Empty state | Simple icon, short explanation, one clear next action. |
| Loading state | Skeleton placeholders or an explicit progress bar. |
| Error state | Human-readable error, retry action, no technical jargon. |
| Bottom navigation | Five destinations, emphasised central Scan, active tab matches the screen. |
| Location selector | Reusable; shows the selected locality and lets the user change it. |
| Impact metric | Consistent typography, units, labels; distinguishes verified values from estimates. |
| Bottom sheet | White, 24px top radius, dim backdrop inside the phone frame, tap-outside to close. Inputs 44px high, 14px radius. |
| Toast | Small forest-green pill at the bottom of the page, auto-dismisses. |

---

## 7. Honesty rules (do not break these)

The UI must not imply that AWS infrastructure, AI classification, maps, provider listings, payments, or collection tracking already work.

- **No fake AI.** Without a real model, the user picks the category with the chips; the result card says **"Chosen by you"** and **"AI classification not connected"**. Never show a fabricated "High confidence" or a made-up classification of a photo.
- **Demo data is labelled** ("Demo data", "Demo provider", "Sample", "Simulated").
- **Simulated actions say so**: collection requests, listings, quotations, and rewards show "Simulated … no collector/dealer has been notified" where relevant.
- **Do not display invented real-world partnerships or fabricated verified providers as real businesses.**
- **Distinguish** AI predictions from confirmed outcomes, and environmental estimates from measured facts.
- No fabricated opening hours, ratings, availability, or live collector locations.
- Show honest loading, error, and empty states.
- Do not promise guaranteed prices or instant payments. Do not estimate weight from a photo.
- The photo stays in the browser (use an object URL); it is never uploaded in the prototype.

---

## 8. Working-prototype behaviour (required)

Implement these with one delegated click handler and small helper functions.

**Navigation**
- Bottom tabs: Home goes to Screen 1, Explore to Screen 4, Scan to Screen 2, Activity to Screen 6, Profile shows a toast "Not part of this prototype".
- Back buttons go to the previous screen in the flow.
- Going to a screen smooth-scrolls it into view and briefly outlines its phone frame (lime outline, about 1.4s, disabled under reduced motion).
- Every "open Screen N" interaction listed in section 5 works.

**Scanner flow**
- "Take a photo" opens a file input with `capture="environment"` (camera on phones). "Upload from gallery" opens a plain image file picker. Reject non-images with a toast.
- On a valid file: show the image in the viewfinder, show a short "Preparing photo…" progress overlay, update to "Step 2 of 3", then fill the photo on Screen 3 and go to Screen 3.
- Category chips on Screen 2 are single-select (default Plastic). Selecting one immediately updates Screen 3.

**Analysis screen**
- Category data for at least: Plastic, Paper, Metal, Glass, E-waste, Organic, Mixed waste. Each has a title, a category label, and advice text. E-waste and hazardous categories use the warning badge and "Needs specialist collection" style guidance.
- Provider cards on Screen 3 show only providers that accept the chosen category, with an empty message when none do.
- "Not right? Change category" returns to Screen 2.
- Share uses `navigator.share` when available, otherwise a toast.
- "Save this report" confirms with a toast ("Report saved for this session").

**Services**
- Filter chips filter provider cards; show an empty state when nothing matches.
- Map / List toggle hides and shows the map.
- Provider card tap opens the detail sheet; "Request a collection" opens the request sheet.
- The request sheet validates pickup location, then shows the confirmation with an `ET-XXXXXX` reference, a "Submitted" badge, and the line "Simulated request. This prototype has no backend, so no collector has been notified." It also updates the Home "Continue your impact" card.

**Marketplace**
- Category chips filter listings, with an empty state.
- "List recyclable waste" opens a sheet (material, approximate quantity in kg, pickup area, optional asking price). Validate that quantity is above 0. On submit, insert the new card at the top of the feed with "Listed" and "Simulated" badges, and switch the filter to that material.
- A dealer can submit a simulated quotation on a listing; the status badge moves through Listed, Quoted, Accepted, and further states, with a rule that a listing cannot be accepted twice. All simulated actions are labelled.

**Impact**
- "Explore more ways to help" opens Screen 5.
- Reward history and the leaderboard are static sample data labelled as such.

**Everything else** that is not part of the prototype (profile page, real search, notifications, real payments) shows the toast **"Not part of this prototype"**.

---

## 9. Technical context (for labels and API-contract awareness only; do not implement)

- **Mobile target:** Kotlin + Jetpack Compose
- **Backend:** Kotlin + Ktor REST API
- **Database:** PostgreSQL on Amazon RDS
- **Image storage:** private Amazon S3 bucket
- **Background processing:** Amazon SQS with worker services
- **Compute:** Amazon ECS with AWS Fargate
- **Authentication:** a secure provider, potentially Amazon Cognito
- **AI:** a replaceable waste-classification model adapter
- **Maps:** MapLibre or another suitable provider

Integrate any of these only through explicit API contracts later. In this prototype, none of them are connected, and the UI must show honest loading, error, and empty states.

---

## 10. Design rules (checklist)

1. Light mode only.
2. One consistent design system across all screens.
3. Lime is the accent, not the background of every component.
4. Realistic photography without colour overlays.
5. No excessive gradients, shadows, or decoration.
6. Do not crowd screens with every possible feature; use sheets and scrolling.
7. Use realistic Indian environmental and recycling contexts.
8. No invented real-world partnerships.
9. No fabricated verified providers presented as real businesses.
10. Clearly identify demo data.
11. Distinguish AI predictions from confirmed outcomes.
12. Distinguish environmental estimates from measured facts.
13. Design for real people and real tasks.
14. Prioritise accessibility, usability, trust, and clear information hierarchy.
15. Keep component sizes, icon styles, spacing, and typography consistent across screens.

---

## 11. Copy and writing rules

- Plain, specific, user-facing language. A user manages waste, not "classification pipelines".
- Active voice. Buttons say exactly what happens ("Submit collection request"), and the same action keeps the same name through the whole flow.
- Errors never apologise and are never vague: say what went wrong and how to fix it.
- Empty screens invite an action.
- Correct spelling and consistent terminology (use "recyclers", "collection request", "verified action" consistently).
- Do not write generic, templated, or AI-sounding marketing copy. Keep it human and short.

---

## 12. Acceptance checklist (verify before finishing)

- [ ] Six phones, 2 rows of 3 on wide screens, identical frames, status bar 9:41, dynamic island.
- [ ] All colours, radii, spacing, and fonts match section 3.
- [ ] Every content item listed in section 5 exists (or is reachable through a sheet or inside-phone scrolling).
- [ ] Bottom nav present on Screens 1, 4, 5, 6 with the correct active tab and a raised lime Scan button.
- [ ] Every status badge has an icon and text.
- [ ] All demo data, simulated actions, and estimates are labelled.
- [ ] No fake AI result, no fake verified businesses, no fabricated hours or ratings.
- [ ] All interactions in section 8 work, including the camera/gallery photo flow, filters, sheets, validation, and confirmation with a tracking reference.
- [ ] No console errors. No remote images. No blocked external resources.
- [ ] Works at desktop width and scales down to a single column on a phone.
- [ ] Final reply is 2–4 sentences: what was built, what is placeholder or simulated, and what assumptions were made.

---

## 13. Reference implementation (working starting point)

This is the working code from the first build. **Use it as the base and extend it; do not restart from scratch and do not change what already matches this brief.** It already has the design tokens, six phone frames, the responsive 2 x 3 grid, bottom nav, the photo flow, category chips, filters, the collection request sheet with a tracking reference, and the marketplace listing sheet.

**What it does not have yet (you must add these to satisfy sections 5 and 8):**
- Real logo mark (it uses a plain leaf icon in a dark circle).
- Provider detail sheet on Screen 4 (tap a card to open it).
- Marketplace listing detail sheet, dealer quotation flow, and transaction-state progression (Listed, Quoted, Accepted, Handover pending, Completed, Disputed), with the rule that a listing cannot be accepted twice.
- Screen 6: separate reported, collected, and confirmed-recycled totals, and the "Campus challenge" leaderboard preview (anonymised sample participants).
- Screen 3: the "uncertain classification" warning card for hazardous or uncertain items, and the "Mixed waste" category chip (the chip row currently shows six categories).
- Loading skeletons and error states beyond the non-image file toast.
- Screen content that overflows 852px must scroll inside the phone, not be cut.
- Real photographs are not possible in a hosted page; the code uses labelled colour-gradient placeholders.

```html
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>EcoTrack – Turn Waste Into Worth</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
<style>
:root{--bg:#F5F6F0;--tx:#171A16;--mu:#777D74;--br:#B9E769;--dk:#263D24;--bd:#E5E8DE;--ok:#3C7950;--wn:#B98532;--tint:#E8F5CC;box-sizing:border-box;padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px);color-scheme:light}
html{scroll-padding-top:env(safe-area-inset-top,0px)}
*,*::before,*::after{box-sizing:border-box;margin:0}
body{font:400 14px/1.4 Inter,system-ui,-apple-system,"Segoe UI",sans-serif;background:#E9EBE3;color:var(--tx)}
button{font:inherit;color:inherit;cursor:pointer}
.top{text-align:center;padding:32px 16px 0}
.top h1{font-size:28px;font-weight:600;letter-spacing:-.02em}
.top p{color:var(--mu);margin-top:6px;max-width:640px;margin-inline:auto}
.grid{display:grid;gap:40px 36px;justify-content:center;padding:28px 16px 56px;--s:1}
.cell h3{font-size:14px;font-weight:600;margin:0 0 14px 6px}
.wrap{width:calc(393px*var(--s));height:calc(852px*var(--s))}
.phone{width:393px;height:852px;border-radius:48px;background:var(--bg);position:relative;overflow:hidden;transform:scale(var(--s));transform-origin:0 0;box-shadow:0 0 0 7px #171A16,0 24px 48px rgba(23,26,22,.18)}
.phone::before{content:"";position:absolute;top:11px;left:50%;width:120px;height:34px;margin-left:-60px;border-radius:20px;background:#171A16;z-index:5}
.sb{height:54px;display:flex;justify-content:space-between;align-items:center;padding:8px 34px 0;font-weight:600;font-size:15px}
.hd{display:flex;align-items:center;justify-content:space-between;height:48px;padding:0 18px;gap:8px}
.hd h2{font-size:17px;font-weight:600}
.rb{width:40px;height:40px;border-radius:50%;background:#fff;border:1px solid var(--bd);display:grid;place-items:center}
.bd{padding:0 18px}
.c{background:#fff;border:1px solid var(--bd);border-radius:18px;padding:14px;box-shadow:0 1px 2px rgba(23,26,22,.04)}
.c.l{border-radius:24px;padding:18px}
.sec{display:flex;justify-content:space-between;align-items:center;margin:16px 0 8px}
.sec h4{font-size:16px;font-weight:600}
.sec a{font-size:13px;font-weight:500;color:var(--ok)}
.mu{color:var(--mu)}.sm{font-size:12px}
.b{display:flex;align-items:center;justify-content:center;gap:8px;height:48px;width:100%;border-radius:999px;font-weight:600;font-size:15px;border:1px solid transparent}
.b.p{background:var(--br);color:var(--dk)}
.b.s{background:#fff;border-color:var(--bd)}
.b.d{background:var(--dk);color:#fff}
.bg{display:inline-flex;align-items:center;gap:4px;height:22px;padding:0 8px;border-radius:99px;font-size:11px;font-weight:500;background:#EEF0E8;white-space:nowrap}
.bg.ok{background:#E3F0E7;color:var(--ok)}
.bg.wn{background:#F6EBD6;color:#8A5F18}
.im{border-radius:14px;position:relative;overflow:hidden;display:flex;align-items:flex-end;padding:6px;color:rgba(255,255,255,.92);font-size:10px;flex:none}
.pl{background:linear-gradient(160deg,#9DB7B4,#5F7F86 55%,#3F5A5E)}
.cb{background:linear-gradient(160deg,#C9A878,#A98457 60%,#7F623F)}
.ew{background:linear-gradient(160deg,#A9ADA6,#797E78 60%,#4E524D)}
.st{background:linear-gradient(160deg,#B8C4A0,#8E9F78 60%,#667555)}
.mt{background:linear-gradient(160deg,#B9B4A8,#8F8A7E 60%,#645F55)}
.row{display:flex;align-items:center;gap:12px}
.row .f{flex:1;min-width:0}
.nm{font-weight:600;font-size:14px}
.chips{display:flex;gap:8px;overflow:hidden;white-space:nowrap}
.ch{height:34px;padding:0 14px;border-radius:99px;background:#fff;border:1px solid var(--bd);display:inline-flex;align-items:center;font-size:13px;font-weight:500}
.ch.on{background:var(--dk);color:#fff;border-color:var(--dk)}
.nv{position:absolute;left:18px;right:18px;bottom:22px;height:64px;background:#fff;border:1px solid var(--bd);border-radius:999px;display:flex;align-items:center;justify-content:space-around;box-shadow:0 8px 24px rgba(23,26,22,.08);z-index:4}
.ni{width:44px;height:44px;border-radius:50%;display:grid;place-items:center;color:var(--mu);background:none;border:0}
.ni.on{background:var(--br);color:var(--dk)}
.ni.sc{width:54px;height:54px;background:var(--br);color:var(--dk);margin-top:-16px;box-shadow:0 0 0 5px #fff,0 0 0 6px var(--bd)}
.vf{height:316px;border-radius:24px;position:relative;background:linear-gradient(160deg,#6E7B66,#434D3E 60%,#2C342A);display:grid;place-items:center;color:rgba(255,255,255,.85);font-size:12px}
.vf s{position:absolute;width:30px;height:30px;border:3px solid #fff;border-radius:8px}
.vf .a{top:20px;left:20px;border-right:0;border-bottom:0}.vf .b2{top:20px;right:20px;border-left:0;border-bottom:0}
.vf .c2{bottom:20px;left:20px;border-right:0;border-top:0}.vf .d2{bottom:20px;right:20px;border-left:0;border-top:0}
.map{height:186px;border-radius:24px;position:relative;overflow:hidden;background:#E9EEDF;border:1px solid var(--bd)}
.map svg{position:absolute;inset:0}
.mk{position:absolute;width:30px;height:30px;border-radius:50%;background:var(--br);border:2px solid #fff;display:grid;place-items:center;color:var(--dk);box-shadow:0 2px 6px rgba(23,26,22,.2)}
.me{position:absolute;width:16px;height:16px;border-radius:50%;background:var(--dk);box-shadow:0 0 0 6px rgba(185,231,105,.6),0 0 0 2px #fff}
.zm{position:absolute;right:10px;top:10px;display:flex;flex-direction:column;background:#fff;border:1px solid var(--bd);border-radius:12px;overflow:hidden}
.zm span{width:34px;height:34px;display:grid;place-items:center}
.tg{position:absolute;left:10px;bottom:10px;display:flex;background:#fff;border:1px solid var(--bd);border-radius:99px;padding:3px;font-size:12px;font-weight:500}
.tg span{padding:4px 12px;border-radius:99px}.tg .on{background:var(--dk);color:#fff}
.bar{height:8px;border-radius:99px;background:#EEF0E8;overflow:hidden}.bar i{display:block;height:100%;background:var(--br);border-radius:99px}
.stat{flex:1;text-align:left}.stat b{display:block;font-size:20px;font-weight:600;letter-spacing:-.01em}.stat span{font-size:11px;color:var(--mu);line-height:1.25;display:block}
.dv{width:1px;align-self:stretch;background:var(--bd)}
.wnc{background:#FBF4E4;border-color:#EBD9B0}
.phone .c.row,.ch{cursor:pointer}
.phone.hl{animation:hl 1.4s}@keyframes hl{0%,60%{outline:4px solid #B9E769;outline-offset:12px}100%{outline:4px solid transparent;outline-offset:12px}}
@media (prefers-reduced-motion:reduce){.phone.hl{animation:none}}
.toast{position:fixed;left:50%;bottom:calc(24px + env(safe-area-inset-bottom,0px));transform:translate(-50%,20px);background:var(--dk);color:#fff;padding:10px 16px;border-radius:99px;font-size:13px;opacity:0;pointer-events:none;transition:.2s;z-index:50;max-width:90vw;text-align:center}.toast.on{opacity:1;transform:translate(-50%,0)}
.ov{position:absolute;inset:0;background:rgba(23,26,22,.4);z-index:8;display:flex;align-items:flex-end}
.sh{background:#fff;border-radius:24px 24px 0 0;padding:20px 18px 30px;width:100%}
.sh label{display:block;font-size:12px;font-weight:500;color:var(--mu);margin:10px 0 4px}
.sh select,.sh input{width:100%;height:44px;border:1px solid var(--bd);border-radius:14px;padding:0 12px;font:inherit;background:#fff;color:var(--tx)}
.up{position:absolute;left:16px;right:16px;bottom:16px;background:rgba(23,26,22,.72);color:#fff;border-radius:14px;padding:10px 12px;display:grid;gap:6px;font-size:12px}.up .bar{background:rgba(255,255,255,.25)}
</style>
</head>
<body>
<header class="top">
<h1>EcoTrack</h1>
<p>Turn Waste Into Worth. Six core screens, one connected flow. All numbers, providers and listings are demo data.</p>
</header>
<main class="grid" id="g">

<section class="cell"><h3>1. Home Dashboard</h3><div class="wrap"><div class="phone">
<div class="hd" style="height:56px">
 <div class="row" style="gap:6px"><span style="width:32px;height:32px;border-radius:50%;background:var(--dk);color:var(--br);display:grid;place-items:center"><i d="leaf" s="18"></i></span><b style="font-weight:600;font-size:17px">EcoTrack</b></div>
 <button class="ch" style="height:34px;gap:4px;padding:0 10px;font-size:12px" aria-label="Change location"><i d="pin" s="14"></i>Kolhapur, Maharashtra<i d="chevd" s="14"></i></button>
 <span style="width:36px;height:36px;border-radius:50%;background:var(--br);color:var(--dk);display:grid;place-items:center;font-weight:600" aria-label="Profile">P</span>
</div>
<div class="bd">
 <h1 style="font-size:23px;font-weight:600;letter-spacing:-.02em;margin-top:6px">Make a little difference today.</h1>
 <p class="mu" style="margin:2px 0 14px">Let's give your waste a better destination.</p>
 <div class="c l" style="background:var(--tint);border-color:#D6E8AE">
  <span style="width:44px;height:44px;border-radius:50%;background:var(--dk);color:var(--br);display:grid;place-items:center"><i d="cam" s="22"></i></span>
  <h3 style="font-size:18px;font-weight:600;margin:12px 0 2px">What are you throwing away?</h3>
  <p class="mu" style="font-size:13px;margin-bottom:14px">Identify waste and find the right way to dispose of it.</p>
  <button class="b d"><i d="cam" s="18"></i>Scan your waste</button>
 </div>
 <div class="sec"><h4>Your impact</h4><span class="bg">Demo data</span></div>
 <div class="c row" style="gap:10px;padding:12px">
  <div class="stat"><b>12 kg</b><span>materials recovered</span></div><div class="dv"></div>
  <div class="stat"><b>8</b><span>actions completed</span></div><div class="dv"></div>
  <div class="stat"><b>5</b><span>collection requests</span></div>
 </div>
 <div class="sec"><h4>Nearby services</h4><a href="#">See all</a></div>
 <div class="chips" style="gap:10px">
  <div class="c row" style="width:262px;padding:10px;flex:none;white-space:normal"><div class="im st" style="width:60px;height:60px"></div><div class="f"><div class="nm">GreenCycle Recycling</div><div class="mu sm">Paper, cardboard, plastics</div><div class="row" style="gap:6px;margin-top:4px"><span class="sm"><i d="pin" s="12"></i> 1.8 km</span><span class="bg ok"><i d="chk" s="11"></i>Verified</span></div></div></div>
  <div class="c row" style="width:262px;padding:10px;flex:none;white-space:normal"><div class="im ew" style="width:60px;height:60px"></div><div class="f"><div class="nm">E-Waste Collect</div><div class="mu sm">Phones, chargers, cables</div><div class="row" style="gap:6px;margin-top:4px"><span class="sm"><i d="pin" s="12"></i> 3.2 km</span><span class="bg">Demo</span></div></div></div>
 </div>
 <div class="sec"><h4>Continue your impact</h4></div>
 <div class="c row" style="padding:10px"><div class="im pl" style="width:44px;height:44px"></div><div class="f"><div class="nm">Plastic bottles report</div><span class="bg wn" style="margin-top:2px"><i d="clk" s="11"></i>Request submitted</span></div><i d="chev" s="18"></i></div>
</div>
<nav class="nv" data-a="0"></nav>
</div></div></section>

<section class="cell"><h3>2. AI Waste Scanner</h3><div class="wrap"><div class="phone">
<div class="hd"><button class="rb" aria-label="Back"><i d="back" s="20"></i></button><h2>Identify waste</h2><button class="rb" aria-label="Help"><i d="help" s="20"></i></button></div>
<div class="bd">
 <div class="row" style="margin:6px 0 12px;gap:10px"><span class="sm" style="font-weight:500;white-space:nowrap">Step 1 of 3</span><div class="row f" style="gap:6px"><i style="flex:1;height:5px;border-radius:9px;background:var(--dk)"></i><i style="flex:1;height:5px;border-radius:9px;background:var(--bd)"></i><i style="flex:1;height:5px;border-radius:9px;background:var(--bd)"></i></div></div>
 <div class="vf"><s class="a"></s><s class="b2"></s><s class="c2"></s><s class="d2"></s><span style="display:flex;flex-direction:column;align-items:center;gap:6px"><i d="cam" s="28"></i>Camera preview</span></div>
 <h3 style="font-size:16px;font-weight:600;margin:16px 0 2px">Take a clear photo of the item or waste pile.</h3>
 <p class="mu" style="font-size:13px;margin-bottom:14px">For mixed waste, photograph the pile from a safe distance.</p>
 <button class="b p"><i d="cam" s="18"></i>Take a photo</button>
 <button class="b s" style="margin-top:8px"><i d="img" s="18"></i>Upload from gallery</button>
 <p class="mu sm row" style="gap:6px;margin:12px 0 8px;align-items:flex-start"><i d="info" s="14"></i><span>Your photo is used only to identify waste and recommend suitable disposal options.</span></p>
 <div class="c wnc row" style="padding:10px 12px;align-items:flex-start;gap:8px;font-size:12px"><i d="warn" s="16"></i><span>Never touch unknown chemicals, sharp objects, or potentially hazardous waste to take a photo.</span></div>
 <div class="chips" style="margin-top:12px"><span class="ch">Plastic</span><span class="ch">Paper</span><span class="ch">Metal</span><span class="ch">Glass</span><span class="ch">E-waste</span><span class="ch">Organic</span></div>
</div>
</div></div></section>

<section class="cell"><h3>3. Waste Analysis &amp; Disposal</h3><div class="wrap"><div class="phone">
<div class="hd"><button class="rb" aria-label="Back"><i d="back" s="20"></i></button><h2>Waste analysis</h2><button class="rb" aria-label="Share"><i d="share" s="20"></i></button></div>
<div class="bd">
 <div class="im pl" style="height:150px;border-radius:24px;margin-top:4px">Photo of submitted waste (placeholder)</div>
 <div class="c" style="margin-top:12px">
  <div class="row" style="justify-content:space-between"><h3 style="font-size:20px;font-weight:600">Plastic bottles</h3><span class="bg">Sample result</span></div>
  <div class="row" style="gap:6px;margin:6px 0"><span class="bg ok"><i d="rec" s="11"></i>Dry recyclable waste</span><span class="bg"><i d="info" s="11"></i>High confidence</span></div>
  <p class="mu" style="font-size:13px">Empty the bottles and keep them separate from food waste. Local recycling options may differ.</p>
  <a href="#" style="display:inline-block;margin-top:6px;font-size:13px;font-weight:600;text-decoration:underline">Not right? Change category</a>
 </div>
 <div class="sec"><h4>Recommended next step</h4></div>
 <button class="b p"><i d="pin" s="18"></i>Find nearby recyclers</button>
 <div class="row" style="gap:8px;margin-top:8px"><button class="b s" style="font-size:14px">Request a collection</button><button class="b s" style="font-size:14px">Save this report</button></div>
 <div class="sec" style="margin-top:14px"><h4>Accepted by nearby services</h4></div>
 <div class="c row" style="padding:10px;margin-bottom:8px"><div class="im st" style="width:44px;height:44px"></div><div class="f"><div class="nm">GreenCycle Recycling</div><div class="mu sm">Plastics · 1.8 km</div></div><span class="bg ok"><i d="chk" s="11"></i>Verified</span></div>
 <div class="c row" style="padding:10px;margin-bottom:10px"><div class="im mt" style="width:44px;height:44px"></div><div class="f"><div class="nm">Metro Scrap Traders</div><div class="mu sm">Plastics · 2.6 km</div></div><span class="bg"><i d="clk" s="11"></i>Hours unknown</span></div>
 <p class="mu sm row" style="gap:6px;align-items:flex-start"><i d="info" s="14"></i><span>AI classification is an estimate. Final acceptance depends on the service provider.</span></p>
</div>
</div></div></section>

<section class="cell"><h3>4. Nearby Services &amp; Collection</h3><div class="wrap"><div class="phone">
<div class="hd"><button class="rb" aria-label="Back"><i d="back" s="20"></i></button><h2>Nearby services</h2><button class="rb" aria-label="Filter"><i d="sl" s="20"></i></button></div>
<div class="bd">
 <div class="c row" style="height:44px;padding:0 14px;border-radius:14px;margin:4px 0 10px"><i d="search" s="18"></i><span class="mu">Search recyclers or collection points</span></div>
 <div class="chips" style="margin-bottom:10px"><span class="ch on">All</span><span class="ch">Scrap dealers</span><span class="ch">Recycling centres</span><span class="ch">E-waste</span></div>
 <div class="map">
  <svg viewBox="0 0 357 186" width="100%" height="100%" aria-hidden="true"><path d="M0 150 Q90 120 170 140 T357 100 V186 H0Z" fill="#D5E4BD"/><path d="M-10 40 Q120 70 200 30 T370 60" stroke="#CFE0E6" stroke-width="14" fill="none"/><g stroke="#fff" stroke-linecap="round" fill="none"><path d="M0 100 H357" stroke-width="9"/><path d="M120 0 V186" stroke-width="9"/><path d="M250 0 L290 186" stroke-width="7"/><path d="M0 160 L357 50" stroke-width="5"/><path d="M40 0 V100" stroke-width="4"/></g></svg>
  <span class="me" style="left:170px;top:84px"></span>
  <span class="mk" style="left:92px;top:60px"><i d="rec" s="15"></i></span><span class="mk" style="left:240px;top:106px"><i d="rec" s="15"></i></span><span class="mk" style="left:200px;top:30px"><i d="rec" s="15"></i></span>
  <div class="zm"><span><i d="plus" s="16"></i></span><span style="border-top:1px solid var(--bd)"><i d="minus" s="16"></i></span></div>
  <div class="tg"><span class="on">Map</span><span>List</span></div>
  <span class="bg" style="position:absolute;right:10px;bottom:10px">Illustrative map</span>
 </div>
 <div class="c row" style="margin-top:12px;padding:10px;align-items:flex-start"><div class="im st" style="width:64px;height:64px"></div><div class="f"><div class="nm">GreenCycle Recycling</div><div class="mu sm">Recycling centre · 1.8 km away</div><div class="mu sm" style="margin:2px 0 4px">Accepts paper, cardboard and selected plastics</div><span class="bg">Demo provider</span></div></div>
 <div class="c row" style="margin-top:8px;padding:10px;align-items:flex-start"><div class="im ew" style="width:64px;height:64px"></div><div class="f"><div class="nm">E-Waste Collect</div><div class="mu sm">E-waste collection · 3.2 km away</div><div class="mu sm" style="margin:2px 0 4px">Phones, chargers, small appliances</div><span class="bg"><i d="clk" s="11"></i>Hours not confirmed</span></div></div>
 <button class="b p" style="margin-top:12px">Request a collection</button>
</div>
<nav class="nv" data-a="1"></nav>
</div></div></section>

<section class="cell"><h3>5. Scrap Marketplace</h3><div class="wrap"><div class="phone">
<div class="hd"><h2 style="font-size:20px">Recycle &amp; earn</h2><div class="row" style="gap:8px"><button class="rb" aria-label="Search"><i d="search" s="20"></i></button><button class="rb" aria-label="Notifications"><i d="bell" s="20"></i></button></div></div>
<div class="bd">
 <p class="mu" style="margin:0 0 10px">Give recyclable materials another life.</p>
 <div class="chips"><span class="ch on">Paper</span><span class="ch">Plastic</span><span class="ch">Metal</span><span class="ch">Glass</span><span class="ch">E-waste</span></div>
 <div class="c row" style="margin:12px 0;padding:10px 12px;align-items:flex-start;gap:8px;font-size:12px;background:var(--tint);border-color:#D6E8AE"><i d="info" s="16"></i><span>Prices and acceptance depend on material quality, quantity, and local demand.</span></div>
 <button class="b p"><i d="plus" s="18"></i>List recyclable waste</button>
 <div class="sec"><h4>Recent listings</h4><span class="bg">Demo data</span></div>
 <div class="c row" style="padding:10px;margin-bottom:8px"><div class="im cb" style="width:68px;height:68px"></div><div class="f"><div class="nm">Cardboard boxes</div><div class="mu sm">Approx. 8 kg · Pickup area: sample location</div><span class="bg wn" style="margin-top:4px"><i d="clk" s="11"></i>Listed</span></div></div>
 <div class="c row" style="padding:10px"><div class="im pl" style="width:68px;height:68px"></div><div class="f"><div class="nm">Plastic bottles</div><div class="mu sm">Approx. 5 kg · Pickup area: sample location</div><span class="bg ok" style="margin-top:4px"><i d="chk" s="11"></i>Quoted</span></div></div>
 <div class="sec"><h4>Registered dealers</h4></div>
 <div class="c row" style="padding:10px"><div class="im mt" style="width:44px;height:44px"></div><div class="f"><div class="nm">Metro Scrap Traders</div><div class="mu sm">Paper, metal · Quotes available</div></div><span class="bg">Demo</span></div>
 <p class="mu sm row" style="gap:6px;margin-top:10px;align-items:flex-start"><i d="info" s="14"></i><span>Final quantity and price must be confirmed by the buyer and seller.</span></p>
</div>
<nav class="nv" data-a="1"></nav>
</div></div></section>

<section class="cell"><h3>6. Impact &amp; Rewards</h3><div class="wrap"><div class="phone">
<div class="hd"><h2 style="font-size:20px">Your impact</h2><button class="rb" aria-label="Settings"><i d="user" s="20"></i></button></div>
<div class="bd">
 <p class="mu" style="margin:0 0 10px">Every verified action counts.</p>
 <div class="c l" style="padding:14px">
  <div class="row"><svg width="64" height="64" viewBox="0 0 64 64" aria-hidden="true"><circle cx="32" cy="32" r="26" fill="none" stroke="#EEF0E8" stroke-width="8"/><circle cx="32" cy="32" r="26" fill="none" stroke="#B9E769" stroke-width="8" stroke-linecap="round" stroke-dasharray="130.7 163.4" transform="rotate(-90 32 32)"/></svg>
  <div class="f"><b style="font-size:22px;font-weight:600">8 verified actions</b><div class="mu sm">of 10 reported · sample data</div></div></div>
  <div class="row" style="gap:10px;margin-top:12px;padding-top:12px;border-top:1px solid var(--bd)"><div class="stat"><b>12 kg</b><span>recovered</span></div><div class="dv"></div><div class="stat"><b>5</b><span>completed collections</span></div><div class="dv"></div><div class="stat"><b>120</b><span>points earned</span></div></div>
 </div>
 <div class="sec" style="margin-top:12px"><h4>Collected by material</h4><span class="bg">Sample</span></div>
 <div class="c" style="padding:12px 14px;display:grid;gap:8px">
  <div class="row"><span class="sm" style="width:50px">Paper</span><div class="bar f"><i style="width:100%"></i></div><b class="sm" style="width:36px;text-align:right">5 kg</b></div>
  <div class="row"><span class="sm" style="width:50px">Plastic</span><div class="bar f"><i style="width:80%"></i></div><b class="sm" style="width:36px;text-align:right">4 kg</b></div>
  <div class="row"><span class="sm" style="width:50px">Metal</span><div class="bar f"><i style="width:40%"></i></div><b class="sm" style="width:36px;text-align:right">2 kg</b></div>
  <div class="row"><span class="sm" style="width:50px">Other</span><div class="bar f"><i style="width:20%"></i></div><b class="sm" style="width:36px;text-align:right">1 kg</b></div>
  <div class="row" style="border-top:1px solid var(--bd);padding-top:8px"><span class="bg"><i d="info" s="11"></i>Estimate</span><span class="sm f">≈ 15 kg CO₂e avoided, from documented factors</span></div>
 </div>
 <div class="sec" style="margin:12px 0 6px"><h4>Reward history</h4></div>
 <div class="c" style="padding:4px 14px">
  <div class="row" style="padding:7px 0;border-bottom:1px solid var(--bd)"><div class="f"><div class="nm" style="font-size:13px">Cardboard handover confirmed</div><div class="mu sm">4 Oct 2026</div></div><b class="sm">+30 pts</b></div>
  <div class="row" style="padding:7px 0"><div class="f"><div class="nm" style="font-size:13px">Plastic collection verified</div><div class="mu sm">22 Sep 2026</div></div><b class="sm">+40 pts</b></div>
 </div>
 <div class="row" style="gap:8px;margin:10px 0">
  <div class="c f" style="padding:8px;text-align:center;font-size:11px;font-weight:500;line-height:1.25"><span style="color:var(--ok)"><i d="chk" s="16"></i></span><br>First verified recycling</div>
  <div class="c f" style="padding:8px;text-align:center;font-size:11px;font-weight:500;line-height:1.25"><span class="mu"><i d="leaf" s="16"></i></span><br>Community contributor</div>
  <div class="c f" style="padding:8px;text-align:center;font-size:11px;font-weight:500;line-height:1.25"><span class="mu"><i d="rec" s="16"></i></span><br>Consistent recycler</div>
 </div>
 <button class="b p" style="height:44px">Explore more ways to help</button>
</div>
<nav class="nv" data-a="3"></nav>
</div></div></section>

</main>
<script>
var P={back:'<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',cam:'<path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/>',house:'<path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .7-1.5l7-6a2 2 0 0 1 2.6 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',compass:'<circle cx="12" cy="12" r="10"/><path d="m16.2 7.8-1.8 5.4a2 2 0 0 1-1.3 1.3L7.8 16.2l1.8-5.4a2 2 0 0 1 1.3-1.3z"/>',act:'<path d="M22 12h-2.5a2 2 0 0 0-1.9 1.5l-2.3 8.4a.25.25 0 0 1-.5 0L9.2 2.2a.25.25 0 0 0-.5 0l-2.3 8.4A2 2 0 0 1 4.5 12H2"/>',user:'<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',help:'<circle cx="12" cy="12" r="10"/><path d="M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3"/><path d="M12 17h.01"/>',share:'<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><path d="m16 6-4-4-4 4"/><path d="M12 2v13"/>',search:'<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',bell:'<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>',sl:'<path d="M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4"/>',pin:'<path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>',chk:'<path d="M3.9 8.6a4 4 0 0 1 4.7-4.8 4 4 0 0 1 6.8 0 4 4 0 0 1 4.8 4.8 4 4 0 0 1 0 6.8 4 4 0 0 1-4.8 4.8 4 4 0 0 1-6.8 0 4 4 0 0 1-4.7-4.8 4 4 0 0 1 0-6.8Z"/><path d="m9 12 2 2 4-4"/>',info:'<circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>',warn:'<path d="m21.7 18-8-14a2 2 0 0 0-3.5 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.7-3"/><path d="M12 9v4M12 17h.01"/>',chev:'<path d="m9 18 6-6-6-6"/>',chevd:'<path d="m6 9 6 6 6-6"/>',plus:'<path d="M5 12h14M12 5v14"/>',minus:'<path d="M5 12h14"/>',leaf:'<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.500-4.800 10-10 10Z"/><path d="M2 21c0-3 1.900-5.400 5.100-6C9.500 14.500 12 13 13 12"/>',img:'<rect width="18" height="18" x="3" y="3" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.100-3.100a2 2 0 0 0-2.800 0L6 21"/>',clk:'<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',rec:'<path d="M7 19H4.800a1.800 1.800 0 0 1-1.600-2.700L5 13.500"/><path d="m11 19 1.500 0a1.800 1.800 0 0 0 1.500-2.700L11.500 12"/><path d="M16 16l3 3-3 3"/><path d="m8.500 6 2.500-4 2.500 4"/><path d="M14.500 8.500 18 6l2.500 4"/>'};
var N=[['house','Home'],['compass','Explore'],['cam','Scan'],['act','Activity'],['user','Profile']];
document.querySelectorAll('.nv').forEach(function(n){var a=+n.dataset.a;n.innerHTML=N.map(function(x,i){return '<button class="ni'+(i==a?' on':'')+(i==2?' sc':'')+'" aria-label="'+x[1]+(i==a?' (current)':'')+'"><i d="'+x[0]+'" s="'+(i==2?24:22)+'"></i></button>'}).join('')});
document.querySelectorAll('.phone').forEach(function(p){p.insertAdjacentHTML('afterbegin','<div class="sb"><span>9:41</span><svg width="68" height="14" viewBox="0 0 68 14" aria-hidden="true"><rect x="0" y="9" width="3" height="5" rx="1"/><rect x="5" y="6" width="3" height="8" rx="1"/><rect x="10" y="3" width="3" height="11" rx="1"/><rect x="15" y="0" width="3" height="14" rx="1"/><rect x="30" y="1" width="34" height="13" rx="4" fill="none" stroke="#171A16" opacity=".4"/><rect x="32" y="3" width="26" height="9" rx="2.500"/></svg></div>')});
document.querySelectorAll('i[d]').forEach(function(e){var s=e.getAttribute('s')||18;e.outerHTML='<svg width="'+s+'" height="'+s+'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="flex:none;vertical-align:-2px">'+P[e.getAttribute('d')]+'</svg>'});
function fit(){var w=innerWidth,c=w>=1380?3:w>=950?2:1,s=Math.min(1,(w-32-(c-1)*36-14)/(c*393)),g=document.getElementById('g');g.style.setProperty('--s',s);g.style.gridTemplateColumns='repeat('+c+',auto)'}
fit();addEventListener('resize',fit);
(function(){
var C=[].slice.call(document.querySelectorAll('.cell')),ph=function(i){return C[i].querySelector('.phone')},$=function(p,s){return [].slice.call(p.querySelectorAll(s))};
var T=document.createElement('div');T.className='toast';T.setAttribute('role','status');document.body.appendChild(T);
var tt,toast=function(m){T.textContent=m;T.classList.add('on');clearTimeout(tt);tt=setTimeout(function(){T.classList.remove('on')},2600)};
var go=function(i){C[i].scrollIntoView({behavior:'smooth',block:'center',inline:'center'});var p=ph(i);p.classList.remove('hl');void p.offsetWidth;p.classList.add('hl')};
var cat='Plastic',img=null,loc='Kolhapur, Maharashtra',notes={};
var K={Plastic:['Plastic bottles','Dry recyclable waste','Empty the bottles and keep them separate from food waste. Local recycling options may differ.'],Paper:['Paper and cardboard','Dry recyclable waste','Keep it dry and flat, and separate from food waste. Local recycling options may differ.'],Metal:['Metal items','Dry recyclable waste','Rinse cans and wrap sharp edges. Local recycling options may differ.'],Glass:['Glass','Dry recyclable waste','Keep glass separate and wrapped so it cannot cut anyone. Local recycling options may differ.'],'E-waste':['Electronic waste','Needs specialist collection','Do not put it in household waste. Hand it to an authorised e-waste collection service.'],Organic:['Organic waste','Wet waste','Keep it separate from dry waste and use a compost or wet-waste collection.']};
var A={'GreenCycle Recycling':['Plastic','Paper'],'Metro Scrap Traders':['Paper','Metal','Plastic']},D={'GreenCycle Recycling':'1.8 km','Metro Scrap Traders':'2.6 km'},S={'GreenCycle Recycling':'Recycling centres','E-Waste Collect':'E-waste'},M={'Cardboard boxes':'Paper','Plastic bottles':'Plastic'},IM={Paper:'cb',Plastic:'pl',Metal:'mt',Glass:'st','E-waste':'ew'};
var o=function(a,v){return a.map(function(x){return '<option'+(x==v?' selected':'')+'>'+x+'</option>'}).join('')};
var ic=function(n,s){return '<svg width="'+s+'" height="'+s+'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="vertical-align:-2px">'+P[n]+'</svg>'};
var nm=function(c){var e=c.querySelector('.nm');return e&&e.textContent},ty=function(c){return c.dataset.m||S[nm(c)]||M[nm(c)]};
function empty(i,msg,anchor,show){var n=notes[i];if(!n){n=notes[i]=document.createElement('p');n.className='mu sm';n.style.cssText='text-align:center;padding:12px 0'}n.textContent=msg;anchor.after(n);n.hidden=!show}
function sheet(i,h){var p=ph(i),v=document.createElement('div');v.className='ov';v.innerHTML='<div class="sh">'+h+'</div>';p.appendChild(v);v.onclick=function(e){if(e.target==v)v.remove()};return v}
function show3(){var k=K[cat],p=ph(2),c=p.querySelector('.c'),b=c.querySelectorAll('.bg');
c.querySelector('h3').textContent=k[0];b[0].textContent='Chosen by you';b[1].lastChild.textContent=k[1];b[1].className='bg '+(cat=='E-waste'?'wn':'ok');b[2].lastChild.textContent='AI classification not connected';c.querySelector('p').textContent=k[2];
var rs=$(p,'.c.row').filter(nm),n=0;rs.forEach(function(r){var a=(A[nm(r)]||[]).indexOf(cat)>-1;r.style.display=a?'':'none';if(a){n++;r.querySelector('.mu.sm').textContent=cat+' · '+D[nm(r)]}});
empty(2,'No demo providers accept this category yet.',rs[rs.length-1],!n);
if(img){var im=p.querySelector('.im');im.style.background='url('+img+') center/cover';im.textContent=''}}
function flt(i,v){var cs=$(ph(i),'.c.row').filter(function(c){return nm(c)&&ty(c)}),n=0;cs.forEach(function(c){var s=v=='All'||ty(c)==v;c.style.display=s?'':'none';if(s)n++});empty(i,i==3?'No demo providers in this category yet.':'No demo listings for this material yet.',cs[cs.length-1],!n)}
function chip(i,ch){[].forEach.call(ch.parentNode.children,function(x){x.classList.remove('on')});ch.classList.add('on');var v=ch.textContent.trim();if(i==1){cat=v;show3()}if(i==3||i==4)flt(i,v)}
var inp=document.createElement('input');inp.type='file';inp.accept='image/*';inp.hidden=true;document.body.appendChild(inp);
inp.onchange=function(){var f=inp.files[0];inp.value='';if(!f)return;if(!/^image\//.test(f.type)){toast('Choose an image file.');return}
if(img)URL.revokeObjectURL(img);img=URL.createObjectURL(f);var vf=ph(1).querySelector('.vf');vf.style.background='url('+img+') center/cover';vf.querySelector('span').style.display='none';
var u=document.createElement('div');u.className='up';u.innerHTML='<span>Preparing photo…</span><div class="bar"><i style="width:0"></i></div>';vf.appendChild(u);var w=0,b=u.querySelector('i'),id=setInterval(function(){w+=14;b.style.width=Math.min(w,100)+'%';if(w>=100){clearInterval(id);u.remove();
$(ph(1),'.sm').forEach(function(x){if(/Step/.test(x.textContent))x.textContent='Step 2 of 3'});ph(1).querySelectorAll('.row.f i')[1].style.background='#263D24';show3();go(2);toast('Photo ready. Check the category it was filed under.')}},120)};
function col(){var s=sheet(3,'<h3 style="font-size:18px;font-weight:600">Request a collection</h3><label>Waste category</label><select>'+o(Object.keys(K),cat)+'</select><label>Approximate quantity</label><select>'+o(['Under 5 kg','5 to 10 kg','More than 10 kg'])+'</select><label>Pickup location</label><input value="'+loc+'"><label>Preferred time</label><select>'+o(['Tomorrow morning','Tomorrow evening','This weekend'])+'</select><label>Notes (optional)</label><input placeholder="Landmark or gate details"><button class="b p" style="margin-top:16px">Submit collection request</button><button class="b s" style="margin-top:8px">Cancel</button>'),f=$(s,'select,input'),bt=$(s,'button');
bt[1].onclick=function(){s.remove()};bt[0].onclick=function(){if(!f[2].value.trim()){toast('Enter a pickup location');return}var r='ET-'+Math.random().toString(36).slice(2,8).toUpperCase(),c=f[0].value;
s.firstChild.innerHTML='<div style="text-align:center"><span style="color:var(--ok)">'+ic('chk',36)+'</span><h3 style="font-size:20px;font-weight:600;margin:6px 0">Request submitted</h3><p class="mu sm">Tracking reference</p><b style="font-size:20px">'+r+'</b><div style="margin:8px 0"><span class="bg wn">'+ic('clk',11)+'Submitted</span></div><p class="mu sm" style="margin:10px 0 16px">Simulated request. This prototype has no backend, so no collector has been notified.</p><button class="b p">Done</button></div>';
s.querySelector('button').onclick=function(){s.remove()};var h=$(ph(0),'.nm').filter(function(x){return /report/.test(x.textContent)})[0];if(h)h.textContent=K[c][0]+' report'}}
function lst(){var s=sheet(4,'<h3 style="font-size:18px;font-weight:600">List recyclable waste</h3><label>Material</label><select>'+o(Object.keys(IM),'Paper')+'</select><label>Approximate quantity (kg)</label><input type="number" min="1" placeholder="e.g. 8"><label>Pickup area</label><input placeholder="Area or landmark"><label>Asking price (optional)</label><input placeholder="Leave blank to invite quotes"><button class="b p" style="margin-top:16px">List waste</button><button class="b s" style="margin-top:8px">Cancel</button>'),f=$(s,'select,input'),bt=$(s,'button');
bt[1].onclick=function(){s.remove()};bt[0].onclick=function(){var m=f[0].value,q=parseFloat(f[1].value);if(!(q>0)){toast('Enter an approximate quantity in kg');return}
var d=document.createElement('div');d.className='c row';d.style.cssText='padding:10px;margin-bottom:8px';d.dataset.m=m;d.innerHTML='<div class="im '+IM[m]+'" style="width:68px;height:68px"></div><div class="f"><div class="nm">'+m+' (your listing)</div><div class="mu sm"></div><span class="bg wn" style="margin-top:4px">'+ic('clk',11)+'Listed</span> <span class="bg" style="margin-top:4px">Simulated</span></div>';
d.querySelector('.mu').textContent='Approx. '+q+' kg · Pickup area: '+(f[2].value.trim()||'Not specified');var first=$(ph(4),'.c.row').filter(nm)[0];first.parentNode.insertBefore(d,first);s.remove();
$(ph(4),'.chips .ch').forEach(function(c){if(c.textContent.trim()==m)chip(4,c)});toast('Listed in this prototype only. No dealer has been notified.')}}
function area(){var s=sheet(0,'<h3 style="font-size:18px;font-weight:600">Choose your area</h3>'+['Kolhapur, Maharashtra','Sangli, Maharashtra','Pune, Maharashtra','Mumbai, Maharashtra'].map(function(c){return '<button class="b s" style="margin-top:8px">'+c+'</button>'}).join(''));
$(s,'button').forEach(function(b){b.onclick=function(){loc=b.textContent;var h=ph(0).querySelector('.hd .ch');[].filter.call(h.childNodes,function(n){return n.nodeType==3})[0].textContent=loc;s.remove()}})}
var NA='Not part of this prototype';
document.addEventListener('click',function(e){var t=e.target;if(t.closest('.ov'))return;var p=t.closest('.phone');if(!p)return;var i=C.indexOf(p.parentNode.parentNode);
var ch=t.closest('.chips .ch');if(ch){chip(i,ch);return}
var tg=t.closest('.tg span');if(tg){$(tg.parentNode,'span').forEach(function(x){x.classList.toggle('on',x==tg)});ph(3).querySelector('.map').style.display=tg.textContent=='List'?'none':'';return}
var n=t.closest('.ni');if(n){var x=[].indexOf.call(n.parentNode.children,n);if(x==4)toast(NA);else go([0,3,1,5][x]);return}
var b=t.closest('button,a,.c.row');if(!b)return;e.preventDefault();var s=b.getAttribute('aria-label')||b.textContent.trim();
if(/Back/.test(s)){go(Math.max(0,i-1));return}
if(i==0){if(/location/.test(s))area();else if(/Scan your/.test(s))go(1);else if(/See all|GreenCycle|E-Waste/.test(s))go(3);else if(/report/.test(s))go(2);else if(/recovered/.test(s))go(5)}
if(i==1){if(/Help/.test(s))toast('Pick a category, then take or upload a photo.');else if(/Take a photo/.test(s)){inp.setAttribute('capture','environment');inp.click()}else if(/Upload/.test(s)){inp.removeAttribute('capture');inp.click()}}
if(i==2){if(/Share/.test(s)){if(navigator.share)navigator.share({text:K[cat][0]+': '+K[cat][1]}).catch(function(){});else toast('Sharing is not available in this browser')}else if(/Find nearby/.test(s))go(3);else if(/Request a collection/.test(s))col();else if(/Save this/.test(s))toast('Report saved for this session');else if(/Change category/.test(s)){go(1);toast('Pick the right category under the camera')}}
if(i==3){if(/Filter/.test(s))toast('Use the chips to filter providers');else if(/Request a collection/.test(s))col()}
if(i==4){if(/Search|Notifications/.test(s))toast(NA);else if(/List recyclable/.test(s))lst()}
if(i==5){if(/Settings/.test(s))toast(NA);else if(/Explore more/.test(s))go(4)}});
ph(1).querySelector('.chips .ch').classList.add('on');
})();
</script>
</body>
</html>
```
