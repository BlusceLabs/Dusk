# Workspace

## Overview

pnpm workspace monorepo using TypeScript. Contains an API server and the Dusk mobile app.

## Stack

- **Monorepo tool**: pnpm workspaces
- **Node.js version**: 24
- **Package manager**: pnpm
- **TypeScript version**: 5.9
- **API framework**: Express 5 (Node.js/TypeScript gateway)
- **Database**: PostgreSQL + Drizzle ORM
- **Validation**: Zod (`zod/v4`), `drizzle-zod`
- **Build**: esbuild (CJS bundle)

### Hybrid Backend Services (all running, all read `$PORT`)
| Service | Language/Framework | Workflow Name | Port | Health |
|---|---|---|---|---|
| ML / Recommendations | Python 3 + Django 5 + DRF | `services/ml: Python Django` | 8000 | `GET /health` |
| Rate Limiter | Rust + Actix-web (token bucket) | `services/rate-limiter: Rust` | 3000 | `GET /health` |
| Realtime / WebSocket | Java 21 + Javalin 6 | `services/realtime: Java` | 3001 | `GET /health` |
| Feed Engine / Fanout | Scala 3 + Cask | `services/feed: Scala` | 3002 | `GET /health` |
| Media Pipeline | C++17 + cpp-httplib + OpenSSL | `services/media: C++` | 3003 | `GET /health` |

Start scripts: `artifacts/<service>/start.sh` (auto-build on first run, `$PORT`-aware)

## Key Commands

- `pnpm run typecheck` — full typecheck across all packages
- `pnpm run build` — typecheck + build all packages
- `pnpm --filter @workspace/api-server run dev` — run API server locally
- `pnpm --filter @workspace/dusk run dev` — run Dusk Expo app locally

See the `pnpm-workspace` skill for workspace structure, TypeScript setup, and package details.

---

## Dusk — Social Super-App

### Project Description
**Dusk** is a comprehensive Android social super-app built with Expo/React Native. It combines features from Twitter, Instagram, Telegram, TikTok, Signal, Patreon, Reddit, Twitch, and more.

### Architecture
- **Frontend**: Expo SDK 54, React Native, Expo Router v6 (file-based routing)
- **Backend**: Firebase (Firestore, Auth, Storage, FCM)
- **Media CDN**: Cloudflare R2 (credentials TBD)
- **State**: React Context (AppContext) + Zustand
- **UI**: Custom design system with warm sunset palette

### Key Files
- `artifacts/dusk/app/_layout.tsx` — Root layout with Firebase auth routing
- `artifacts/dusk/app/(tabs)/` — Main tab screens (Feed, Communities, Notifications, Messages, Profile)
- `artifacts/dusk/app/auth/index.tsx` — Firebase auth screen (login + register)
- `artifacts/dusk/context/AppContext.tsx` — Global state with Firebase auth integration
- `artifacts/dusk/lib/firebase/config.ts` — Firebase initialization with AsyncStorage persistence
- `artifacts/dusk/lib/firebase/auth.ts` — Auth service (login, register, sign out)
- `artifacts/dusk/lib/firebase/firestore.ts` — Firestore CRUD for posts, messages, users, communities
- `artifacts/dusk/lib/firebase/storage.ts` — Firebase Storage upload utilities
- `artifacts/dusk/components/ComposeModal.tsx` — Post composer with Firestore + image picker
- `artifacts/dusk/constants/colors.ts` — Design tokens (dark/light theme)
- `artifacts/dusk/hooks/useColors.ts` — Theme hook
- `artifacts/dusk/hooks/useAuth.ts` — Firebase auth state hook

### Firebase Config
- Project ID: `duskapp26`
- Package: `com.dusk.messenger`
- Auth domain: `duskapp26.firebaseapp.com`
- Sender ID: `925483369290`
- Env vars: `EXPO_PUBLIC_FIREBASE_*` set in Replit secrets

### Backend Services (API Server)
- **Cloudflare R2** — media storage with presigned upload URLs (`/api/media`)
- **Neon PostgreSQL** — users, follows, subscriptions schema (`/api/social`)
- **Astra DataStax (Cassandra)** — feed interactions, analytics
- **Upstash Redis** — caching, rate limiting, live viewer counts, trending hashtags
- **Redis Streams** (via Upstash) — real-time event streaming (replaces Kafka): streams `dusk:stream:events`, `dusk:stream:analytics`, `dusk:stream:notifications`, `dusk:stream:feed-fanout`, `dusk:stream:moderation`
- **RabbitMQ** (CloudAMQP) — job queues: `dusk.push.notifications`, `dusk.email.transactional`, `dusk.media.process`, `dusk.messages.deliver`, `dusk.feed.update`

### Event Pipeline (`/api/events/*`)
Events flow through two systems simultaneously:
1. **Redis Streams** — persistent ordered log (analytics, monitoring, audit trail)
2. **RabbitMQ queues** — actionable jobs (push notifications, media processing, DM delivery, feed fan-out)

Key event handlers in `artifacts/api-server/src/lib/events.ts`:
- `onPostCreated` → stream + RabbitMQ feed fan-out + hashtag trending
- `onPostLiked` → stream + push notification queue
- `onUserFollowed` → stream + push notification queue
- `onUserRegistered` → stream + welcome email queue
- `onMessageSent` → stream + RabbitMQ deliver queue
- `onMediaUploaded` → RabbitMQ process queue (compress/transcode/nsfw-check)
- `onContentFlagged` → moderation stream

### Features Built
- **Auth**: Email/password sign-in and registration with Firebase Auth, AsyncStorage persistence
- **Feed**: "For You", "Following", "Live" tabs; stories bar; post cards; reels strip
- **Posts**: Like, repost, bookmark (optimistic + Firestore sync); compose with image picker; Firestore real-time post subscription
- **Comments**: Firestore real-time comment subscription in post detail; live comment posting
- **Reels tab** (`/(tabs)/reels`): TikTok-style vertical video feed as a dedicated tab; `subscribeToVideoPosts` wired from Firestore with mock fallback; double-tap hearts, comments drawer, follow button, vinyl animation, volume toggle
- **Stories**: Instagram-style stories with progress bar and auto-advance (`/story/[id]`)
- **Communities**: Browse, join/leave; Reddit-like detail screen with Posts/About/Rules tabs; PostCard community tag navigates to community detail
- **Messages**: Real-time Firestore chat with typing indicators, read receipts, emoji reactions, reply-to, deletion, date separators; accessible from DM icon in Feed header
- **Notifications**: Filterable notification feed; wired to Firestore real-time subscription with mock fallback; badge in Notifications tab
- **Profile**: User stats, posts/media/bookmarks tabs, Creator Hub, theme toggle, sign-out; Edit Profile screen; Subscribe button for creator profiles routes to `/subscribe/[creatorId]`
- **Live Streaming browse** (`/streams`): Browse all live Firestore streams; category filter (All/Gaming/Music/Art/IRL/Talk/Fitness/Tech); featured stream hero + grid; live viewer pulse animation; Go Live shortcut; routes to `/stream/[id]`
- **Stream viewer** (`/stream/[id]`): Full-screen stream viewer with live chat; Go Live screen (`/go-live`) with category picker, Firestore stream creation, live controls
- **Search/Explore tab** (`/(tabs)/search`): Dedicated Explore tab; real Firestore user search + trending hashtags from Redis; "People to Follow" with follow toggle; "Live Now" shortcut to `/streams`; multi-tab results (Top/People/Posts/Communities)
- **Story creation**: StoryBar "Your Story" button opens ImagePicker → uploads to R2 → creates Firestore frame; viewer can visit their own story after publishing
- **Creator Hub** (`/creator-hub`): Subscription tiers (Bronze/Silver/Gold), revenue analytics, subscriber activity; wired to Firestore `subscribeToCreatorTiers` + `subscribeToCreatorActivity`
- **Creator Subscriptions** (`/subscribe/[creatorId]`): Patreon-style tier selection; monthly/yearly toggle (20% off yearly); PayPal checkout routing; wired to Firestore creator tiers

### Tab Bar (5 tabs)
Feed (Home) | Search/Explore | Reels | Notifications | Profile
- Messages: accessible via DM icon (💬) in Feed header
- Communities: accessible via Search tab (Communities results tab) and direct URL
- Live: accessible via 📻 icon in Feed header → `/streams`

### Security
- **Firebase ID token verification**: API server middleware (`artifacts/api-server/src/middleware/auth.ts`) verifies JWT via Google JWKs; applied to all write routes (follow, unfollow, interactions, events, payments, push register)
- **Client token forwarding**: `lib/api.ts` `request()` function fetches Firebase ID token from `auth.currentUser?.getIdToken()` and sends it as `Authorization: Bearer <token>` on every API call
- **Firestore security rules**: Production-grade rules written to `FIRESTORE_SECURITY_RULES.md` — copy into Firebase Console to lock down collections
- **Rate limiting**: 300 req/min global, per-action limits on interactions/follow/push

### Data — Fixed Issues
- Comments: start empty from Firestore (mock COMMENTS array removed); loading state while fetching; like persistence via Firestore
- Feed: empty state when no posts ("Nothing here yet" + compose CTA)
- Search: reads `?q=` URL param on mount (bio hashtag/mention taps now pre-populate query)
- Comments: `incrementCommentCount` + `likeComment` functions added to firestore.ts

### Design System
- Primary: `#ff8c5a` (dark mode) / `#ff6b35` (light mode)
- Background: `#0d0b10` (dark) / `#f8f7f4` (light)
- Card: `#1c1a20` (dark) / `#ffffff` (light)
- Typography: Inter (400, 500, 600, 700)
- Verified badge: `#1d9bf0`
- Gold (creator): `#f4d03f`

### Important Rules
- NEVER use `app.config.ts/js` — only `app.json` for Expo config
- Auth screen route: `/auth`
- Main app route: `/(tabs)`
- `babel-preset-expo` must be pinned to `~54.0.10` for expo 54 compatibility
