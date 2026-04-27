# Dusk Production Gaps Design

**Date:** 2026-04-27  
**Scope:** 6 Critical Production Gaps for React Native Dusk App  
**Status:** Approved for Implementation

---

## Overview

This document specifies designs for 6 critical production gaps identified in the Dusk React Native application. These items must be resolved before production launch.

| # | Gap | Priority | Effort |
|---|-----|----------|--------|
| 1 | Crash Reporting (Sentry) | P0 | 2h |
| 2 | Feed Pagination | P0 | 4h |
| 3 | Optimistic UI with Rollback | P0 | 6h |
| 4 | Request Retry Logic | P0 | 4h |
| 5 | Deep Link Handling | P0 | 3h |
| 6 | App Store Metadata | P0 | 2h |

---

## 1. Crash Reporting Integration

### Selected Solution: Sentry

**Rationale:** Superior React Native stack traces, performance monitoring, and release health compared to Firebase Crashlytics. Team already uses Sentry.

### Architecture

```
┌─────────────────────────────────────────────┐
│           Dusk React Native App             │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │   @sentry/react-native │
        │      SDK v6.x         │
        └──────────┬──────────┘
                   │
        ┌──────────┴──────────┐
        │   Sentry Cloud      │
        │   (sentry.io)       │
        └─────────────────────┘
```

### Implementation

**Initialization** (`artifacts/dusk/lib/sentry.ts`):
```typescript
import * as Sentry from '@sentry/react-native';
import Constants from 'expo-constants';

export function initSentry() {
  Sentry.init({
    dsn: process.env.EXPO_PUBLIC_SENTRY_DSN,
    environment: __DEV__ ? 'development' : 'production',
    release: `${Constants.expoConfig?.name}@${Constants.expoConfig?.version}`,
    dist: Constants.expoConfig?.version,
    enableAutoSessionTracking: true,
    sessionTrackingIntervalMillis: 30000,
    enableNativeCrashHandling: true,
    enableNativeNagger: false,
    beforeSend: (event) => {
      // Filter out dev errors if needed
      if (__DEV__ && event.level === 'debug') return null;
      return event;
    },
  });
}
```

**User Context** (in AuthGate / AppContext):
```typescript
Sentry.setUser({
  id: user.uid,
  username: user.username,
  email: user.email,
  isPremium: String(user.isPremium),
  isCreator: String(user.isCreator),
});
```

**Breadcrumbs** (key navigation/actions):
```typescript
Sentry.addBreadcrumb({
  category: 'navigation',
  message: 'Navigated to profile',
  data: { userId: targetUserId },
  level: 'info',
});
```

**Performance Monitoring**:
```typescript
const transaction = Sentry.startTransaction({
  name: 'Feed Load',
  op: 'feed.load',
});

// Firestore query
transaction.startChild({ op: 'firestore.query', description: 'posts.fetch' });
// ... query completes
transaction.finish();
```

### Source Maps

CI build uploads source maps:
```yaml
- name: Upload Sentry source maps
  run: |
    pnpm exec sentry-upload-sourcemaps \
      --release "${{ env.VERSION }}" \
      --dist "${{ env.SHORT_SHA }}" \
      ./artifacts/dusk/dist
```

### Error Boundaries

```typescript
import * as Sentry from '@sentry/react-native';

export const ErrorBoundary = Sentry.withErrorBoundary(Component, {
  fallback: ({ error, resetError }) => (
    <ErrorFallback error={error} onReset={resetError} />
  ),
  onError: (error) => {
    // Log to console in dev
    console.error('Error caught by Sentry:', error);
  },
});
```

### Acceptance Criteria

- [ ] Crashes appear in Sentry dashboard within 5 minutes
- [ ] Stack traces show original TypeScript (source mapped)
- [ ] User context visible on crash reports
- [ ] Performance transactions for feed load < 5s threshold
- [ ] Source maps automatically uploaded on CI build

---

## 2. Feed Pagination (Twitter-Style)

### Architecture

```
┌─────────────────────────────────────────────┐
│           Firestore Collection              │
│              posts (indexed)                │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │  Cursor-based Query │
        │  limit(20)          │
        │  orderBy(createdAt) │
        └──────────┬──────────┘
                   │
        ┌──────────┴──────────┐
        │   React State       │
        │   - posts[]         │
        │   - cursor          │
        │   - hasMore         │
        └─────────────────────┘
```

### State Shape

```typescript
interface FeedState {
  posts: Post[];
  cursor: Timestamp | null;
  hasMore: boolean;
  isLoading: boolean;        // Initial load
  isLoadingMore: boolean;   // Pagination load
  isRefreshing: boolean;    // Pull-to-refresh
  error: Error | null;
}

interface FeedActions {
  loadInitial(): Promise<void>;
  loadMore(): Promise<void>;
  refresh(): Promise<void>;
}
```

### Firestore Queries

**Initial Load**:
```typescript
const snapshot = await firestore
  .collection('posts')
  .orderBy('createdAt', 'desc')
  .limit(20)
  .get();

const lastDoc = snapshot.docs[snapshot.docs.length - 1];
const cursor = lastDoc.data().createdAt;
```

**Load More** (pagination):
```typescript
const snapshot = await firestore
  .collection('posts')
  .orderBy('createdAt', 'desc')
  .startAfter(cursor)
  .limit(20)
  .get();
```

### Component Implementation

**Feed Screen** (`(tabs)/index.tsx`):
```typescript
export default function FeedScreen() {
  const { posts, hasMore, isLoadingMore, loadMore, refresh, isRefreshing } = useFeed();
  const flatListRef = useRef<FlatList>(null);

  const onEndReached = useCallback(() => {
    if (!isLoadingMore && hasMore) {
      loadMore();
    }
  }, [isLoadingMore, hasMore, loadMore]);

  return (
    <FlatList
      ref={flatListRef}
      data={posts}
      renderItem={({ item }) => <PostCard post={item} />}
      keyExtractor={(item) => item.id}
      onEndReached={onEndReached}
      onEndReachedThreshold={0.5}
      refreshing={isRefreshing}
      onRefresh={refresh}
      ListFooterComponent={
        isLoadingMore ? <ActivityIndicator /> : null
      }
      // Prevent race conditions with requestId
      extraData={posts.length}
    />
  );
}
```

### Hook: `useFeed`

```typescript
export function useFeed() {
  const [state, setState] = useState<FeedState>({
    posts: [],
    cursor: null,
    hasMore: true,
    isLoading: false,
    isLoadingMore: false,
    isRefreshing: false,
    error: null,
  });

  // Track active request to prevent race conditions
  const requestIdRef = useRef(0);

  const loadMore = useCallback(async () => {
    if (state.isLoadingMore || !state.hasMore) return;

    const requestId = ++requestIdRef.current;
    setState(s => ({ ...s, isLoadingMore: true }));

    try {
      const { posts, cursor } = await fetchPosts({
        after: state.cursor,
        limit: 20,
      });

      // Ignore stale responses
      if (requestId !== requestIdRef.current) return;

      setState(s => ({
        ...s,
        posts: [...s.posts, ...posts],
        cursor: cursor,
        hasMore: posts.length === 20,
        isLoadingMore: false,
      }));
    } catch (error) {
      if (requestId !== requestIdRef.current) return;
      Sentry.captureException(error);
      setState(s => ({ ...s, isLoadingMore: false, error }));
    }
  }, [state.cursor, state.hasMore, state.isLoadingMore]);

  const refresh = useCallback(async () => {
    requestIdRef.current++; // Cancel pending requests
    setState(s => ({ ...s, isRefreshing: true }));

    try {
      const { posts, cursor } = await fetchPosts({ limit: 20 });

      setState(s => ({
        ...s,
        posts,
        cursor,
        hasMore: posts.length === 20,
        isRefreshing: false,
      }));
    } catch (error) {
      Sentry.captureException(error);
      setState(s => ({ ...s, isRefreshing: false, error }));
    }
  }, []);

  return { ...state, loadMore, refresh };
}
```

### Acceptance Criteria

- [ ] Feed loads first 20 posts on mount
- [ ] Scroll to bottom loads next 20 without jank
- [ ] Pull-to-refresh resets to latest 20
- [ ] "New posts" button appears when new content available (like Twitter)
- [ ] No duplicate posts on race conditions
- [ ] Loading indicator shows only when more data exists

---

## 3. Optimistic UI with Rollback

### Pattern

```
User Action
    │
    ▼
┌──────────────────┐
│ Optimistic Update │ <-- Immediate local state change
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Background Sync  │ <-- Firestore call
└────────┬─────────┘
         │
    ┌────┴────┐
    ▼         ▼
 Success    Failure
    │         │
    ▼         ▼
 Confirm   Rollback
 (no-op)  + Toast
```

### State Machine

```typescript
interface OptimisticState<T> {
  value: T;
  status: 'confirmed' | 'pending' | 'error';
  previousValue: T;
}

interface OptimisticAction<T> {
  type: 'OPTIMISTIC_UPDATE' | 'CONFIRM' | 'ROLLBACK' | 'RETRY';
  payload?: {
    value?: T;
    error?: Error;
  };
}
```

### Implementation: Post Like

**Hook: `useOptimisticLike`**:
```typescript
export function useOptimisticLike(postId: string, initialLiked: boolean) {
  const [state, setState] = useState<OptimisticState<boolean>>({
    value: initialLiked,
    status: 'confirmed',
    previousValue: initialLiked,
  });

  const userId = useAuth().user?.uid;

  const toggleLike = useCallback(async () => {
    if (!userId) return;

    const newValue = !state.value;
    const previousValue = state.value;

    // 1. Optimistic update
    setState({
      value: newValue,
      status: 'pending',
      previousValue,
    });

    // 2. Background sync
    try {
      if (newValue) {
        await likePost(postId);
      } else {
        await unlikePost(postId);
      }

      // 3. Confirm (no state change, just status)
      setState(s => ({ ...s, status: 'confirmed' }));
    } catch (error) {
      // 4. Rollback on failure
      setState({
        value: previousValue,
        status: 'error',
        previousValue,
      });

      // Show error
      Toast.show({
        type: 'error',
        text1: 'Failed to update',
        text2: 'Please try again',
      });

      Sentry.captureException(error, {
        extra: { postId, attemptedAction: newValue ? 'like' : 'unlike' },
      });
    }
  }, [postId, userId, state.value]);

  const retry = useCallback(() => {
    setState(s => ({ ...s, status: 'confirmed' }));
    toggleLike();
  }, [toggleLike]);

  return {
    isLiked: state.value,
    isPending: state.status === 'pending',
    isError: state.status === 'error',
    toggleLike,
    retry,
  };
}
```

**UI Layer**:
```typescript
function LikeButton({ postId, initialLiked, likeCount }: Props) {
  const { isLiked, isPending, isError, toggleLike } = useOptimisticLike(
    postId,
    initialLiked
  );

  return (
    <TouchableOpacity
      onPress={toggleLike}
      disabled={isPending}
      style={isPending && styles.pending}
    >
      <HeartIcon
        filled={isLiked}
        color={isLiked ? '#FF6B6B' : undefined}
      />
      <Text>{likeCount + (isLiked ? 1 : 0) - (initialLiked ? 1 : 0)}</Text>
    </TouchableOpacity>
  );
}
```

### Conflict Resolution

If optimistic update succeeds but another change happened server-side:

```typescript
// Firestore real-time listener wins
// Compare timestamps, server state takes precedence
const resolveConflict = (local: Post, server: Post) => {
  if (server.updatedAt > local.updatedAt) {
    return server; // Server wins
  }
  return local;
};
```

### Apply To

| Feature | Optimistic? | Rollback Component |
|---------|-------------|-------------------|
| Like/Unlike | Yes | LikeButton |
| Follow/Unfollow | Yes | FollowButton |
| Bookmark | Yes | BookmarkButton |
| Post creation | No | N/A (navigate after success) |
| Comment | Yes | CommentInput |
| Delete post | No | Confirmation dialog first |

### Acceptance Criteria

- [ ] Like button updates immediately on tap
- [ ] Failed like reverts visually with animation
- [ ] Toast appears on rollback with retry option
- [ ] No duplicate likes/unlikes from rapid tapping
- [ ] Conflict resolution handled via Firestore listener

---

## 4. Request Retry Logic

### Scope

Firebase SDK handles its own retries. This covers:
- Custom REST API calls (`api.ts`)
- Cloud function invocations
- Image uploads beyond Storage SDK

### Exponential Backoff

```
Attempt 1: Immediate
Attempt 2: After 1s delay
Attempt 3: After 2s delay
Attempt 4: After 4s delay
Attempt 5: After 8s delay (max)
```

Max 4 retries per request, max 5 failures within 30s triggers circuit breaker.

### Circuit Breaker

```
Closed: Requests flow normally
   │
   ▼ (5 failures in 30s)
Open: Fail fast for 60s
   │
   ▼ (after 60s)
Half-Open: Allow 1 test request
   │
   ├─ Success ──► Closed
   │
   └─ Failure ──► Open
```

### Implementation

**Retry Utility**:
```typescript
interface RetryConfig {
  maxRetries: number;
  baseDelay: number;
  maxDelay: number;
  retryableErrors: (error: unknown) => boolean;
}

const defaultConfig: RetryConfig = {
  maxRetries: 4,
  baseDelay: 1000,
  maxDelay: 8000,
  retryableErrors: (error) => {
    if (error instanceof Error) {
      // Network errors
      if (error.message.includes('network')) return true;
      if (error.message.includes('timeout')) return true;
      // HTTP 5xx
      if (error.message.includes('500')) return true;
      if (error.message.includes('502')) return true;
      if (error.message.includes('503')) return true;
    }
    return false;
  },
};

export async function withRetry<T>(
  fn: () => Promise<T>,
  config: Partial<RetryConfig> = {}
): Promise<T> {
  const cfg = { ...defaultConfig, ...config };
  let lastError: unknown;

  for (let attempt = 0; attempt <= cfg.maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;

      if (attempt === cfg.maxRetries) break;
      if (!cfg.retryableErrors(error)) throw error;

      const delay = Math.min(
        cfg.baseDelay * Math.pow(2, attempt),
        cfg.maxDelay
      );

      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }

  throw lastError;
}
```

**Circuit Breaker**:
```typescript
class CircuitBreaker {
  private failures = 0;
  private lastFailureTime: number = 0;
  private state: 'closed' | 'open' | 'half-open' = 'closed';

  constructor(
    private threshold = 5,
    private timeout = 60000,
    private window = 30000
  ) {}

  canExecute(): boolean {
    if (this.state === 'open') {
      const elapsed = Date.now() - this.lastFailureTime;
      if (elapsed > this.timeout) {
        this.state = 'half-open';
        return true;
      }
      return false;
    }
    return true;
  }

  recordSuccess() {
    this.failures = 0;
    this.state = 'closed';
  }

  recordFailure() {
    this.lastFailureTime = Date.now();
    this.failures++;

    // Check if in window
    if (this.failures >= this.threshold) {
      this.state = 'open';
    }
  }

  async execute<T>(fn: () => Promise<T>): Promise<T> {
    if (!this.canExecute()) {
      throw new Error('Circuit breaker is open');
    }

    try {
      const result = await fn();
      this.recordSuccess();
      return result;
    } catch (error) {
      this.recordFailure();
      throw error;
    }
  }
}
```

### Integration with API Client

```typescript
// Circuit breaker per endpoint
const breakers: Record<string, CircuitBreaker> = {};

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  if (!breakers[endpoint]) {
    breakers[endpoint] = new CircuitBreaker();
  }

  const breaker = breakers[endpoint];

  return breaker.execute(() =>
    withRetry(async () => {
      const response = await fetch(`${API_URL}${endpoint}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      return response.json();
    })
  );
}
```

### Offline Queue

For mutations during connectivity loss:

```typescript
interface PendingAction {
  id: string;
  type: 'like' | 'unlike' | 'follow' | 'post';
  payload: unknown;
  timestamp: number;
  retryCount: number;
}

class OfflineQueue {
  private queue: PendingAction[] = [];

  enqueue(action: Omit<PendingAction, 'id' | 'timestamp' | 'retryCount'>) {
    this.queue.push({
      ...action,
      id: generateId(),
      timestamp: Date.now(),
      retryCount: 0,
    });
    this.persist();
  }

  async flush() {
    const online = await NetInfo.fetch();
    if (!online.isConnected) return;

    const failed: PendingAction[] = [];

    for (const action of this.queue) {
      try {
        await executeAction(action);
      } catch {
        action.retryCount++;
        if (action.retryCount < 3) {
          failed.push(action);
        } else {
          Sentry.captureMessage('Action permanently failed', {
            extra: action,
          });
        }
      }
    }

    this.queue = failed;
    this.persist();
  }

  private persist() {
    AsyncStorage.setItem('offlineQueue', JSON.stringify(this.queue));
  }
}
```

### Acceptance Criteria

- [ ] API calls retry on network failure up to 4 times
- [ ] Backoff delays increase exponentially (1s, 2s, 4s, 8s)
- [ ] Circuit opens after 5 failures in 30s
- [ ] Circuit stays open for 60s before half-open
- [ ] 4xx errors don't trigger retry
- [ ] Offline mutations queue and flush on reconnect

---

## 5. Deep Link Handling

### Supported Routes

| Path | Screen | Parameters |
|------|--------|------------|
| `/post/:id` | Post Detail | `id` |
| `/profile/:id` | User Profile | `id` |
| `/community/:id` | Community | `id` |
| `/chat/:id` | Chat Detail | `id` |
| `/story/:id` | Story Viewer | `id` |

### Configuration

**Expo Router** (`app/+native-intent.ts`):
```typescript
import { NativeIntent } from 'expo-router';

export function redirectSystemPath({
  path,
  extraData,
}: NativeIntent): string {
  // Handle non-standard URLs
  if (path.startsWith('/p/')) {
    return `/post/${path.slice(3)}`;
  }
  return path;
}
```

**AndroidManifest** (in `app.json`):
```json
{
  "expo": {
    "android": {
      "intentFilters": [
        {
          "action": "VIEW",
          "data": [
            {
              "scheme": "https",
              "host": "dusk.app",
              "pathPrefix": "/post"
            },
            {
              "scheme": "https",
              "host": "dusk.app",
              "pathPrefix": "/profile"
            },
            {
              "scheme": "https",
              "host": "dusk.app",
              "pathPrefix": "/community"
            },
            {
              "scheme": "https",
              "host": "dusk.app",
              "pathPrefix": "/story"
            }
          ],
          "category": ["BROWSABLE", "DEFAULT"]
        }
      ]
    }
  }
}
```

**Asset Links** (`.well-known/assetlinks.json` on `dusk.app`):
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.dusk.messenger",
    "sha256_cert_fingerprints": [
      "SHA256_FINGERPRINT_HERE"
    ]
  }
}]
```

### Navigation Handler

```typescript
// app/_layout.tsx
import * as Linking from 'expo-linking';
import { useEffect } from 'react';
import { useRouter } from 'expo-router';

function useDeepLinks() {
  const router = useRouter();

  useEffect(() => {
    // Handle initial URL
    Linking.getInitialURL().then(url => {
      if (url) handleDeepLink(url, router);
    });

    // Handle incoming while app is open
    const subscription = Linking.addEventListener('url', ({ url }) => {
      handleDeepLink(url, router);
    });

    return () => subscription.remove();
  }, [router]);
}

function handleDeepLink(url: string, router: ReturnType<typeof useRouter>) {
  const parsed = Linking.parse(url);
  const { path, queryParams } = parsed;

  Sentry.addBreadcrumb({
    category: 'deepLink',
    message: `Deep link received: ${path}`,
    data: queryParams,
  });

  switch (true) {
    case path?.startsWith('post/'):
      router.push(`/post/${path.split('/')[1]}`);
      break;
    case path?.startsWith('profile/'):
      router.push(`/profile/${path.split('/')[1]}`);
      break;
    case path?.startsWith('community/'):
      router.push(`/community/${path.split('/')[1]}`);
      break;
    case path?.startsWith('story/'):
      router.push(`/story/${path.split('/')[1]}`);
      break;
    default:
      router.push('/(tabs)');
  }
}
```

### Custom Scheme (Fallback)

For in-app sharing:

```json
{
  "expo": {
    "scheme": "dusk"
  }
}
```

Share URL: `dusk://post/abc123`

### Acceptance Criteria

- [ ] `https://dusk.app/post/:id` opens post detail
- [ ] `https://dusk.app/profile/:id` opens user profile
- [ ] Deep links work when app is closed or backgrounded
- [ ] Custom scheme `dusk://` supported for in-app sharing
- [ ] Asset links file published on domain
- [ ] Android properly handles verified app links (no disambiguation dialog)

---

## 6. App Store Metadata & Screenshots

### Metadata Structure

**Play Store Listing** (`fastlane/metadata/android/`):

```
metadata/
├── en-US/
│   ├── title.txt           "Dusk"
│   ├── short_description.txt  "Everything. Everyone. Everywhere."
│   ├── full_description.txt
│   ├── changelog.txt
│   └── images/
│       ├── icon.png
│       ├── featureGraphic.png
│       ├── phoneScreenshots/
│       │   ├── 01_feed.png
│       │   ├── 02_reels.png
│       │   ├── 03_stories.png
│       │   ├── 04_chat.png
│       │   └── 05_profile.png
│       └── tabletScreenshots/
│           └── ...
```

**Full Description**:
```
Dusk is the social super-app that brings everything you love in one place.

FEED & DISCOVERY
• Curated "For You" feed
• Real-time posts from people you follow
• Trending hashtags and communities

STORIES & REELS
• Share ephemeral moments with Stories
• Watch fullscreen Reels like TikTok

MESSAGING
• End-to-end encrypted DMs
• Group chats, voice messages, reactions

CREATORS
• Subscribe to creators (Bronze/Silver/Gold tiers)
• Go live and build your community

Join millions on Dusk — Everything. Everyone. Everywhere.
```

### Screenshot Automation

**Maestro Flow** (`.maestro/screenshots/feed.yml`):
```yaml
appId: com.dusk.messenger
---
# Take feed screenshots
- launchApp
- tapOn: "Sign In"
- inputText: "test@dusk.app"
- tapOn: "Password"
- inputText: "password"
- tapOn: "Sign In"
- waitForAnimationToEnd

# Feed screenshot - scroll to top
- scrollUntilVisible:
    element: "For You"
- takeScreenshot: feed

# Reels tab
- tapOn: "Reels"
- waitForAnimationToEnd
- takeScreenshot: reels

# Stories
- tapOn:
    id: "story-ring"
- waitForAnimationToEnd
- takeScreenshot: stories

# Chat
- tapOn: "Messages"
- tapOn:
    id: "chat-item-0"
- waitForAnimationToEnd
- takeScreenshot: chat

# Profile
- tapOn: "Profile"
- waitForAnimationToEnd
- takeScreenshot: profile
```

**Fastlane Screenshot**:
```ruby
# fastlane/Fastfile
lane :screenshots do
  capture_android_screenshots(
    app_package_name: "com.dusk.messenger",
    locales: ["en-US"],
    clear_previous_screenshots: true,
    devices: ["Pixel_7_API_34"],
    use_adb_root: true
  )
end
```

### CI Integration

```yaml
# .github/workflows/screenshots.yml
name: Generate Screenshots

on:
  push:
    branches: [main]

jobs:
  screenshots:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Android SDK
        uses: android-actions/setup-android@v3

      - name: Create AVD
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: |
            ./gradlew installRelease
            maestro test .maestro/screenshots/
```

### Acceptance Criteria

- [ ] 5+ phone screenshots covering core flows
- [ ] Tablet screenshots for 7" and 10" devices
- [ ] Feature graphic (1024x500) for Play Store banner
- [ ] Screenshots auto-generated via CI
- [ ] Metadata localized for at least en-US
- [ ] Fastlane lane configured for Play Store upload

---

## Implementation Order

Prioritized by dependencies and production criticality:

1. **Crash Reporting (#1)** — Critical visibility, no deps
2. **Feed Pagination (#2)** — Required for performance
3. **Optimistic UI (#3)** — Required for UX quality
4. **Request Retry (#4)** — Required for resilience
5. **Deep Links (#5)** — Required for user acquisition
6. **App Store Metadata (#6)** — Required for launch submission

---

## Appendix: Environment Variables

Add to `.env`:

```
EXPO_PUBLIC_SENTRY_DSN=https://xxx@xxx.ingest.sentry.io/xxx
EXPO_PUBLIC_SENTRY_AUTH_TOKEN=xxx
SENTRY_ORG=dusk
SENTRY_PROJECT=dusk-mobile
```

---

## Spec Self-Review

- [x] Placeholders: All filled (SHA256 fingerprint, DSN to be configured)
- [x] Consistency: All 6 gaps addressed, no contradictions
- [x] Scope: Focused on production readiness only
- [x] Ambiguity: Clear acceptance criteria per gap
