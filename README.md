# Dusk

**Everything. Everyone. Everywhere.**

Dusk is an open-source Android social super-app built with Expo and React Native. It combines the best features of Twitter, Instagram, Telegram, TikTok, Signal, Patreon, Reddit, and Twitch into a single, unified experience.

---

## Features

- **Feed** — For You / Following / Live tabs, stories bar, post cards, reels strip
- **Reels** — TikTok-style vertical video feed with double-tap to like and comment drawer
- **Stories** — Instagram-style stories with progress bar and auto-advance
- **Posts** — Rich composer with images, video, and polls; real-time Firestore feed
- **Comments** — Real-time comments with likes
- **Communities** — Reddit-style communities with posts, rules, and member counts
- **Messages** — DMs with typing indicators, read receipts, emoji reactions, reply-to, and message deletion
- **Live Streaming** — Twitch-style stream viewer with live chat; go-live flow with category picker
- **Search & Explore** — Multi-tab search (people, posts, communities) with Redis-powered trending hashtags
- **Notifications** — Real-time notification feed with mark-all-read
- **Creator Hub** — Patreon-style subscription tiers (Bronze / Silver / Gold) with revenue analytics
- **Premium** — PayPal-powered paid subscriptions with gold verification badge
- **Profile** — Banner cover photo, 3-column media grid, edit profile, follow/unfollow
- **Dark / Light theme** — System-aware with manual toggle

---

## Tech Stack

### Mobile App
| Layer | Technology |
|-------|-----------|
| Framework | Expo SDK 54, React Native |
| Routing | Expo Router v6 (file-based) |
| State | React Context + Zustand |
| Auth | Firebase Authentication |
| Database | Firebase Firestore (real-time) |
| Media | Cloudflare R2 (via presigned URLs) |
| Payments | PayPal SDK |
| Push | Expo Notifications + FCM |
| UI | Custom design system, Inter font |

### Backend (Hybrid Multi-Language Architecture)

Dusk's backend is built as a polyglot system — different layers are implemented in the language best suited for their workload:

| Service | Language / Framework | Role |
|---------|---------------------|------|
| API Gateway | Node.js 24, Express 5, TypeScript | REST API, auth middleware, event routing |
| Media Pipeline | C++ | High-performance video transcoding and image compression |
| Feed Engine | Scala | Distributed feed fan-out and ranking (Akka Streams) |
| ML / Recommendations | Python, Django | Content recommendations, NSFW detection, hashtag trending |
| Real-time Engine | Java | WebSocket hub, live stream session management |
| Systems Services | Rust | Rate limiting daemon, token validation, hot-path caching |

### Backend Infrastructure
| Layer | Technology |
|-------|-----------|
| Database | Neon PostgreSQL + Drizzle ORM |
| Cache | Upstash Redis (rate limiting, trending, live counts) |
| Queues | RabbitMQ / CloudAMQP (push, email, media processing) |
| Analytics | DataStax Astra (Cassandra) |
| Email | Resend |
| Auth middleware | Firebase ID token verification (Google JWKs) |
| Media CDN | Cloudflare R2 |

---

## Getting Started

### Prerequisites

- Node.js 20+
- pnpm 9+
- Expo Go app on your Android device (or an emulator)
- A Firebase project
- (Optional) Neon, Upstash, Cloudflare R2, RabbitMQ accounts for full backend features

### 1. Clone the repo

```bash
git clone https://github.com/hoodDevs/Dusk.git
cd Dusk
```

### 2. Install dependencies

```bash
pnpm install
```

### 3. Configure environment variables

```bash
cp .env.example .env
```

Open `.env` and fill in your credentials. At minimum you need the **Firebase** variables to run the app. See `.env.example` for descriptions of every variable.

### 4. Start the API server

```bash
pnpm --filter @workspace/api-server run dev
```

### 5. Start the Expo app

```bash
pnpm --filter @workspace/dusk run dev
```

Scan the QR code with Expo Go on your Android device.

---

## Project Structure

```
/
├── artifacts/
│   ├── api-server/          # Express API server
│   │   └── src/
│   │       ├── routes/      # REST endpoints
│   │       ├── lib/         # Events, DB, Redis, RabbitMQ
│   │       └── middleware/  # Firebase auth verification
│   └── dusk/                # Expo React Native app
│       ├── app/             # Expo Router screens
│       │   ├── (tabs)/      # Main tab screens
│       │   ├── auth/        # Sign in / sign up
│       │   ├── chat/        # DM screens
│       │   ├── profile/     # Public profile
│       │   └── ...
│       ├── components/      # Shared UI components
│       ├── context/         # AppContext (global state)
│       ├── lib/
│       │   ├── firebase/    # Firestore, Auth, Storage helpers
│       │   └── api.ts       # API client
│       ├── hooks/           # useColors, useAuth, etc.
│       └── constants/       # Colors, theme tokens
```

---

## Firebase Setup

1. Create a project at [console.firebase.google.com](https://console.firebase.google.com)
2. Enable **Authentication** (Email/Password + Google)
3. Enable **Firestore Database**
4. Enable **Storage**
5. Copy your web app credentials into `.env`

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first.

1. Fork the repo
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit your changes
4. Push and open a PR

---

## License

MIT
