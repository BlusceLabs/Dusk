# Dusk Production Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 6 critical production gaps: Sentry crash reporting, feed pagination, optimistic UI, retry logic, deep links, and store metadata

**Architecture:** React Native with Expo Router, Firebase Firestore for data, Zustand for state, Sentry for monitoring. Each gap addressed as self-contained feature with independent testing.

**Tech Stack:** React Native 0.74, Expo SDK 54, Firebase v10, TanStack Query, TypeScript 5.9

---

## File Mapping

### New Files to Create

| File | Purpose |
|------|---------|
| `artifacts/dusk/lib/sentry.ts` | Sentry initialization and utilities |
| `artifacts/dusk/lib/sentry.config.ts` | Sentry configuration constants |
| `artifacts/dusk/hooks/usePagination.ts` | Generic pagination hook |
| `artifacts/dusk/lib/pagination.ts` | Pagination cursor utilities |
| `artifacts/dusk/hooks/useOptimisticMutation.ts` | Optimistic UI hook |
| `artifacts/dusk/lib/retry.ts` | Retry with exponential backoff |
| `artifacts/dusk/lib/circuit-breaker.ts` | Circuit breaker implementation |
| `artifacts/dusk/lib/deep-links.ts` | Deep link configuration and handlers |
| `artifacts/dusk/lib/offline-queue.ts` | Offline action queue |
| `fastlane/metadata/` | Play Store metadata files |
| `.maestro/screenshots/` | Maestro test flows |

### Files to Modify

| File | Lines | Changes |
|------|-------|---------|
| `artifacts/dusk/app/_layout.tsx` | 1-50 | Add Sentry and deep link handlers |
| `artifacts/dusk/app/(tabs)/index.tsx` | All | Replace with paginated feed |
| `artifacts/dusk/hooks/usePosts.ts` | All | Add pagination logic |
| `artifacts/dusk/components/PostCard.tsx` | 30-60 | Add optimistic like handling |
| `artifacts/dusk/lib/api.ts` | All | Add retry wrapper |
| `artifacts/dusk/context/AppContext.tsx` | 50-100 | Add network monitoring |
| `artifacts/dusk/package.json` | 20-40 | Add Sentry dependency |
| `artifacts/dusk/app.json` | 20-50 | Add deep link intent filters |

---

## Task 1: Install Sentry Dependencies

**Files:**
- Modify: `artifacts/dusk/package.json`

- [ ] **Step 1: Add Sentry packages**
  ```json
  {
    "dependencies": {
      "@sentry/react-native": "~5.24.0"
    }
  }
  ```

- [ ] **Step 2: Install dependencies**
  Run: `cd artifacts/dusk && pnpm install`
  Expected: Packages install without errors

- [ ] **Step 3: Configure Sentry init**
  Create: `artifacts/dusk/lib/sentry.config.ts`
  ```typescript
  export const SENTRY_DSN = process.env.EXPO_PUBLIC_SENTRY_DSN ?? '';
  export const SENTRY_ENV = __DEV__ ? 'development' : 'production';
  ```

- [ ] **Step 4: Add environment variables**
  Create: `artifacts/dusk/.env.sentry` (template)
  ```
  EXPO_PUBLIC_SENTRY_DSN=https://xxx@xxx.ingest.sentry.io/xxx
  ```

- [ ] **Step 5: Commit**
  ```bash
  git add artifacts/dusk/package.json artifacts/dusk/pnpm-lock.yaml
  git add artifacts/dusk/lib/sentry.config.ts
  git commit -m "deps: add Sentry React Native SDK"
  ```

---

## Task 2: Sentry Initialization

**Files:**
- Create: `artifacts/dusk/lib/sentry.ts`
- Modify: `artifacts/dusk/app/_layout.tsx:1-30`

- [ ] **Step 1: Write Sentry init module**
  Create: `artifacts/dusk/lib/sentry.ts`
  ```typescript
  import * as Sentry from '@sentry/react-native';
  import Constants from 'expo-constants';
  import { SENTRY_DSN, SENTRY_ENV } from './sentry.config';

  export function initSentry() {
    if (!SENTRY_DSN) {
      console.warn('Sentry DSN not configured');
      return;
    }

    Sentry.init({
      dsn: SENTRY_DSN,
      environment: SENTRY_ENV,
      release: `${Constants.expoConfig?.name}@${Constants.expoConfig?.version}`,
      dist: Constants.expoConfig?.version,
      enableAutoSessionTracking: true,
      sessionTrackingIntervalMillis: 30000,
      enableNativeCrashHandling: true,
      tracesSampleRate: 0.1,
      beforeSend: (event) => {
        if (__DEV__ && event.level === 'debug') return null;
        return event;
      },
    });
  }

  export function setUserContext(user: { id: string; username?: string; email?: string }) {
    Sentry.setUser({
      id: user.id,
      username: user.username,
      email: user.email,
    });
  }

  export function addBreadcrumb(category: string, message: string, data?: Record<string, unknown>) {
    Sentry.addBreadcrumb({
      category,
      message,
      data,
      level: 'info',
    });
  }

  export const captureException = Sentry.captureException;
  export const startTransaction = Sentry.startTransaction;
  ```

- [ ] **Step 2: Initialize in layout**
  Modify: `artifacts/dusk/app/_layout.tsx`
  Add above RootLayout component:
  ```typescript
  import { initSentry } from '@/lib/sentry';
  initSentry();
  ```

- [ ] **Step 3: Test Sentry init**
  Run: `cd artifacts/dusk && pnpm run typecheck`
  Expected: No TypeScript errors

- [ ] **Step 4: Commit**
  ```bash
  git add artifacts/dusk/lib/sentry.ts artifacts/dusk/app/_layout.tsx
  git commit -m "feat(sentry): initialize crash reporting"
  ```

---

## Task 3: User Context in Sentry

**Files:**
- Modify: `artifacts/dusk/hooks/useAuth.ts:40-60`

- [ ] **Step 1: Import Sentry utilities**
  Add to `artifacts/dusk/hooks/useAuth.ts`:
  ```typescript
  import { setUserContext, captureException } from '@/lib/sentry';
  ```

- [ ] **Step 2: Set user context on auth change**
  Add in auth state effect:
  ```typescript
  useEffect(() => {
    if (user) {
      setUserContext({
        id: user.uid,
        username: user.username,
        email: user.email,
      });
    } else {
      Sentry.setUser(null);
    }
  }, [user]);
  ```

- [ ] **Step 3: Add error capture**
  In error handlers:
  ```typescript
  catch (error) {
    captureException(error, {
      extra: { context: 'auth' },
    });
    // existing error handling
  }
  ```

- [ ] **Step 4: Commit**
  ```bash
  git add artifacts/dusk/hooks/useAuth.ts
  git commit -m "feat(sentry): add user context and auth error capture"
  ```

---

## Task 4: Pagination Utilities

**Files:**
- Create: `artifacts/dusk/lib/pagination.ts`
- Create: `artifacts/dusk/hooks/usePagination.ts`

- [ ] **Step 1: Write pagination types and utils**
  Create: `artifacts/dusk/lib/pagination.ts`
  ```typescript
  import type { Timestamp } from 'firebase/firestore';

  export interface PaginationState<T> {
    items: T[];
    cursor: Timestamp | null;
    hasMore: boolean;
    isLoading: boolean;
    isLoadingMore: boolean;
    isRefreshing: boolean;
    error: Error | null;
  }

  export interface PaginationActions {
    loadInitial(): Promise<void>;
    loadMore(): Promise<void>;
    refresh(): Promise<void>;
  }

  export const PAGE_SIZE = 20;

  export function createInitialState<T>(): PaginationState<T> {
    return {
      items: [],
      cursor: null,
      hasMore: true,
      isLoading: false,
      isLoadingMore: false,
      isRefreshing: false,
      error: null,
    };
  }
  ```

- [ ] **Step 2: Write usePagination hook**
  Create: `artifacts/dusk/hooks/usePagination.ts`
  ```typescript
  import { useState, useCallback, useRef } from 'react';
  import type { Timestamp } from 'firebase/firestore';
  import {
    PaginationState,
    PaginationActions,
    PAGE_SIZE,
    createInitialState,
  } from '@/lib/pagination';
  import { captureException } from '@/lib/sentry';

  interface UsePaginationOptions<T> {
    fetchPage: (params: { cursor?: Timestamp | null; limit: number }) => Promise<{
      items: T[];
      cursor: Timestamp | null;
    }>;
  }

  export function usePagination<T>({ fetchPage }: UsePaginationOptions<T>) {
    const [state, setState] = useState<PaginationState<T>>(createInitialState<T>());
    const requestIdRef = useRef(0);

    const loadInitial = useCallback(async () => {
      requestIdRef.current++;
      setState(s => ({ ...s, isLoading: true, error: null }));

      try {
        const { items, cursor } = await fetchPage({ cursor: null, limit: PAGE_SIZE });
        
        setState({
          items,
          cursor,
          hasMore: items.length === PAGE_SIZE,
          isLoading: false,
          isLoadingMore: false,
          isRefreshing: false,
          error: null,
        });
      } catch (error) {
        captureException(error);
        setState(s => ({ ...s, isLoading: false, error: error as Error }));
      }
    }, [fetchPage]);

    const loadMore = useCallback(async () => {
      if (state.isLoadingMore || !state.hasMore || state.isLoading) return;

      const requestId = ++requestIdRef.current;
      setState(s => ({ ...s, isLoadingMore: true, error: null }));

      try {
        const { items, cursor } = await fetchPage({ cursor: state.cursor, limit: PAGE_SIZE });

        if (requestId !== requestIdRef.current) return;

        setState(s => ({
          ...s,
          items: [...s.items, ...items],
          cursor,
          hasMore: items.length === PAGE_SIZE,
          isLoadingMore: false,
        }));
      } catch (error) {
        if (requestId !== requestIdRef.current) return;
        captureException(error);
        setState(s => ({ ...s, isLoadingMore: false, error: error as Error }));
      }
    }, [fetchPage, state.cursor, state.hasMore, state.isLoading]);

    const refresh = useCallback(async () => {
      requestIdRef.current++;
      setState(s => ({ ...s, isRefreshing: true, error: null }));

      try {
        const { items, cursor } = await fetchPage({ cursor: null, limit: PAGE_SIZE });

        setState({
          items,
          cursor,
          hasMore: items.length === PAGE_SIZE,
          isLoading: false,
          isLoadingMore: false,
          isRefreshing: false,
          error: null,
        });
      } catch (error) {
        captureException(error);
        setState(s => ({ ...s, isRefreshing: false, error: error as Error }));
      }
    }, [fetchPage]);

    return {
      ...state,
      loadInitial,
      loadMore,
      refresh,
    };
  }
  ```

- [ ] **Step 3: Type check**
  Run: `cd artifacts/dusk && pnpm run typecheck`
  Expected: No errors

- [ ] **Step 4: Commit**
  ```bash
  git add artifacts/dusk/lib/pagination.ts artifacts/dusk/hooks/usePagination.ts
  git commit -m "feat(pagination): add generic pagination hook"
  ```

---

## Task 5: Paginated Feed Screen

**Files:**
- Modify: `artifacts/dusk/app/(tabs)/index.tsx`
- Modify: `artifacts/dusk/lib/firebase/posts.ts`

- [ ] **Step 1: Create paginated fetcher**
  Modify: `artifacts/dusk/lib/firebase/posts.ts`
  Add:
  ```typescript
  import {
    collection,
    query,
    orderBy,
    limit,
    startAfter,
    getDocs,
    Timestamp,
  } from 'firebase/firestore';
  import { Post } from '@/domain/model/Post';
  import { db } from './config';

  export async function fetchFeedPosts({
    cursor,
    limit: pageSize,
  }: {
    cursor?: Timestamp | null;
    limit: number;
  }): Promise<{ items: Post[]; cursor: Timestamp | null }> {
    const postsRef = collection(db, 'posts');
    let q = query(postsRef, orderBy('createdAt', 'desc'), limit(pageSize));

    if (cursor) {
      q = query(postsRef, orderBy('createdAt', 'desc'), startAfter(cursor), limit(pageSize));
    }

    const snapshot = await getDocs(q);
    const posts = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    })) as Post[];

    const lastDoc = snapshot.docs[snapshot.docs.length - 1];
    const nextCursor = lastDoc?.data().createdAt as Timestamp | null;

    return { items: posts, cursor: nextCursor };
  }
  ```

- [ ] **Step 2: Update feed screen with pagination**
  Modify: `artifacts/dusk/app/(tabs)/index.tsx`
  ```typescript
  import { FlatList, ActivityIndicator, RefreshControl } from 'react-native';
  import { useCallback, useEffect } from 'react';
  import { usePagination } from '@/hooks/usePagination';
  import { fetchFeedPosts } from '@/lib/firebase/posts';
  import { PostCard } from '@/components/PostCard';
  import type { Post } from '@/domain/model/Post';

  export default function FeedScreen() {
    const {
      items: posts,
      hasMore,
      isLoadingMore,
      isRefreshing,
      loadInitial,
      loadMore,
      refresh,
    } = usePagination<Post>({ fetchPage: fetchFeedPosts });

    useEffect(() => {
      loadInitial();
    }, [loadInitial]);

    const onEndReached = useCallback(() => {
      if (hasMore && !isLoadingMore) {
        loadMore();
      }
    }, [hasMore, isLoadingMore, loadMore]);

    return (
      <FlatList
        data={posts}
        renderItem={({ item }) => <PostCard post={item} />}
        keyExtractor={(item) => item.id}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.5}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={refresh} />
        }
        ListFooterComponent={
          isLoadingMore ? <ActivityIndicator style={{ padding: 16 }} /> : null
        }
        contentContainerStyle={{ paddingVertical: 8 }}
      />
    );
  }
  ```

- [ ] **Step 3: Test pagination**
  Run: `cd artifacts/dusk && pnpm run typecheck`
  Expected: No errors

- [ ] **Step 4: Commit**
  ```bash
  git add artifacts/dusk/lib/firebase/posts.ts
  git add artifacts/dusk/app/(tabs)/index.tsx
  git commit -m "feat(feed): implement cursor-based pagination"
  ```

---

## Task 6: Optimistic Mutation Hook

**Files:**
- Create: `artifacts/dusk/hooks/useOptimisticMutation.ts`

- [ ] **Step 1: Write optimistic mutation hook**
  Create: `artifacts/dusk/hooks/useOptimisticMutation.ts`
  ```typescript
  import { useState, useCallback } from 'react';
  import { captureException } from '@/lib/sentry';

  interface OptimisticState<T> {
    value: T;
    status: 'confirmed' | 'pending' | 'error';
    previousValue: T;
  }

  interface UseOptimisticMutationOptions<T, R = void> {
    optimisticUpdate: (current: T) => T;
    onSubmit: () => Promise<R>;
    onSuccess?: (result: R) => void;
    onError?: (error: Error) => void;
  }

  export function useOptimisticMutation<T>(initialValue: T) {
    const [state, setState] = useState<OptimisticState<T>>({
      value: initialValue,
      status: 'confirmed',
      previousValue: initialValue,
    });

    const mutate = useCallback(async <R = void>(
      options: UseOptimisticMutationOptions<T, R>
    ): Promise<void> => {
      const { optimisticUpdate, onSubmit, onSuccess, onError } = options;
      const previousValue = state.value;

      // 1. Optimistic update
      setState({
        value: optimisticUpdate(previousValue),
        status: 'pending',
        previousValue,
      });

      // 2. Background sync
      try {
        const result = await onSubmit();
        
        // 3. Confirm
        setState(s => ({ ...s, status: 'confirmed' }));
        onSuccess?.(result);
      } catch (error) {
        // 4. Rollback
        setState({
          value: previousValue,
          status: 'error',
          previousValue,
        });
        
        captureException(error);
        onError?.(error as Error);
      }
    }, [state.value]);

    const retry = useCallback(() => {
      setState(s => ({ ...s, status: 'confirmed' }));
    }, []);

    return {
      value: state.value,
      isPending: state.status === 'pending',
      isError: state.status === 'error',
      mutate,
      retry,
    };
  }
  ```

- [ ] **Step 2: Type check**
  Run: `cd artifacts/dusk && pnpm run typecheck`
  Expected: No errors

- [ ] **Step 3: Commit**
  ```bash
  git add artifacts/dusk/hooks/useOptimisticMutation.ts
  git commit -m "feat(optimistic): add optimistic mutation hook with rollback"
  ```

---

## Task 7: Optimistic Like Button

**Files:**
- Modify: `artifacts/dusk/components/PostCard.tsx:30-80`
- Modify: `artifacts/dusk/lib/firebase/posts.ts`

- [ ] **Step 1: Add like operations to posts.ts**
  Modify: `artifacts/dusk/lib/firebase/posts.ts`
  Add:
  ```typescript
  import { doc, updateDoc, increment } from 'firebase/firestore';

  export async function likePost(postId: string): Promise<void> {
    const postRef = doc(db, 'posts', postId);
    await updateDoc(postRef, {
      likes: increment(1),
    });
  }

  export async function unlikePost(postId: string): Promise<void> {
    const postRef = doc(db, 'posts', postId);
    await updateDoc(postRef, {
      likes: increment(-1),
    });
  }
  ```

- [ ] **Step 2: Update PostCard with optimistic like**
  Modify: `artifacts/dusk/components/PostCard.tsx`
  Import and use optimistic mutation:
  ```typescript
  import { useOptimisticMutation } from '@/hooks/useOptimisticMutation';
  import { likePost, unlikePost } from '@/lib/firebase/posts';
  import { useAuth } from '@/hooks/useAuth';
  import Toast from 'react-native-toast-message';

  function LikeButton({ postId, initialLiked, likeCount }: Props) {
    const { user } = useAuth();
    const { value: isLiked, isPending, mutate } = useOptimisticMutation(initialLiked);

    const handlePress = async () => {
      if (!user) {
        Toast.show({ type: 'error', text1: 'Please sign in' });
        return;
      }

      await mutate({
        optimisticUpdate: (current) => !current,
        onSubmit: async () => {
          if (isLiked) {
            await unlikePost(postId);
          } else {
            await likePost(postId);
          }
        },
        onError: () => {
          Toast.show({
            type: 'error',
            text1: 'Failed to update',
            text2: 'Please try again',
          });
        },
      });
    };

    return (
      <TouchableOpacity onPress={handlePress} disabled={isPending}>
        <HeartIcon filled={isLiked} color={isLiked ? '#FF6B6B' : undefined} />
        <Text>{likeCount + (isLiked && !initialLiked ? 1 : 0) - (!isLiked && initialLiked ? 1 : 0)}</Text>
      </TouchableOpacity>
    );
  }
  ```

- [ ] **Step 3: Commit**
  ```bash
  git add artifacts/dusk/components/PostCard.tsx artifacts/dusk/lib/firebase/posts.ts
  git commit -m "feat(optimistic): add optimistic like with rollback"
  ```

---

## Task 8: Retry Logic Implementation

**Files:**
- Create: `artifacts/dusk/lib/retry.ts`
- Create: `artifacts/dusk/lib/circuit-breaker.ts`

- [ ] **Step 1: Write retry utility**
  Create: `artifacts/dusk/lib/retry.ts`
  ```typescript
  export interface RetryConfig {
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
        if (error.message.includes('network')) return true;
        if (error.message.includes('timeout')) return true;
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

        const delay = Math.min(cfg.baseDelay * Math.pow(2, attempt), cfg.maxDelay);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    throw lastError;
  }
  ```

- [ ] **Step 2: Write circuit breaker**
  Create: `artifacts/dusk/lib/circuit-breaker.ts`
  ```typescript
  type CircuitState = 'closed' | 'open' | 'half-open';

  export class CircuitBreaker {
    private failures = 0;
    private lastFailureTime = 0;
    private state: CircuitState = 'closed';

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

    recordSuccess(): void {
      this.failures = 0;
      this.state = 'closed';
    }

    recordFailure(): void {
      this.lastFailureTime = Date.now();
      this.failures++;

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

- [ ] **Step 3: Commit**
  ```bash
  git add artifacts/dusk/lib/retry.ts artifacts/dusk/lib/circuit-breaker.ts
  git commit -m "feat(resilience): add retry logic and circuit breaker"
  ```

---

## Task 9: API Client with Retry

**Files:**
- Modify: `artifacts/dusk/lib/api.ts`

- [ ] **Step 1: Add retry wrapper to API client**
  Modify: `artifacts/dusk/lib/api.ts`
  ```typescript
  import { withRetry } from './retry';
  import { CircuitBreaker } from './circuit-breaker';

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
            Authorization: `Bearer ${await getAuthToken()}`,
            'Content-Type': 'application/json',
            ...options.headers,
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        return response.json();
      })
    );
  }
  ```

- [ ] **Step 2: Commit**
  ```bash
  git add artifacts/dusk/lib/api.ts
  git commit -m "feat(api): add retry and circuit breaker to API client"
  ```

---

## Task 10: Deep Link Configuration

**Files:**
- Create: `artifacts/dusk/lib/deep-links.ts`
- Modify: `artifacts/dusk/app.json:20-50`
- Modify: `artifacts/dusk/app/_layout.tsx:1-50`

- [ ] **Step 1: Write deep link types and config**
  Create: `artifacts/dusk/lib/deep-links.ts`
  ```typescript
  export const DEEP_LINK_ROUTES = {
    post: '/post/:id',
    profile: '/profile/:id',
    community: '/community/:id',
    story: '/story/:id',
    chat: '/chat/:id',
  } as const;

  export type DeepLinkPath = keyof typeof DEEP_LINK_ROUTES;

  export function parseDeepLink(url: string): { path: string; params: Record<string, string> } | null {
    try {
      const parsed = new URL(url);
      const pathname = parsed.pathname;
      const searchParams = Object.fromEntries(parsed.searchParams.entries());

      return { path: pathname, params: searchParams };
    } catch {
      // Handle custom scheme
      if (url.startsWith('dusk://')) {
        const path = url.replace('dusk://', '');
        return { path: `/${path}`, params: {} };
      }
      return null;
    }
  }

  export function getRouteFromPath(path: string): string | null {
    const segments = path.split('/').filter(Boolean);
    if (segments.length < 2) return null;

    const [type, id] = segments;
    
    switch (type) {
      case 'post': return `/post/${id}`;
      case 'profile': return `/profile/${id}`;
      case 'community': return `/community/${id}`;
      case 'story': return `/story/${id}`;
      case 'chat': return `/chat/${id}`;
      default: return null;
    }
  }
  ```

- [ ] **Step 2: Configure Android intent filters**
  Modify: `artifacts/dusk/app.json`
  Add under `expo.android`:
  ```json
  {
    "expo": {
      "scheme": "dusk",
      "android": {
        "intentFilters": [
          {
            "action": "VIEW",
            "autoVerify": true,
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
              },
              {
                "scheme": "https",
                "host": "dusk.app",
                "pathPrefix": "/chat"
              }
            ],
            "category": ["BROWSABLE", "DEFAULT"]
          }
        ]
      }
    }
  }
  ```

- [ ] **Step 3: Commit**
  ```bash
  git add artifacts/dusk/lib/deep-links.ts artifacts/dusk/app.json
  git commit -m "feat(deep-links): add deep link configuration and routing"
  ```

---

## Task 11: Deep Link Handler

**Files:**
- Modify: `artifacts/dusk/app/_layout.tsx:1-50`

- [ ] **Step 1: Add deep link handling to layout**
  Modify: `artifacts/dusk/app/_layout.tsx`
  ```typescript
  import * as Linking from 'expo-linking';
  import { useEffect } from 'react';
  import { useRouter } from 'expo-router';
  import { parseDeepLink, getRouteFromPath } from '@/lib/deep-links';
  import { addBreadcrumb } from '@/lib/sentry';

  function useDeepLinks() {
    const router = useRouter();

    useEffect(() => {
      // Handle initial URL
      const handleInitialURL = async () => {
        const url = await Linking.getInitialURL();
        if (url) handleDeepLink(url, router);
      };
      handleInitialURL();

      // Handle incoming while app is open
      const subscription = Linking.addEventListener('url', ({ url }) => {
        handleDeepLink(url, router);
      });

      return () => subscription.remove();
    }, [router]);
  }

  function handleDeepLink(url: string, router: ReturnType<typeof useRouter>) {
    addBreadcrumb('deepLink', `Deep link received: ${url}`);

    const parsed = parseDeepLink(url);
    if (!parsed) return;

    const route = getRouteFromPath(parsed.path);
    if (route) {
      router.push(route as any);
    }
  }
  ```

- [ ] **Step 2: Call useDeepLinks in layout**
  Add inside RootLayout:
  ```typescript
  function RootLayoutNav() {
    useDeepLinks();
    // ... rest of navigation
  }
  ```

- [ ] **Step 3: Commit**
  ```bash
  git add artifacts/dusk/app/_layout.tsx
  git commit -m "feat(deep-links): add deep link handler and navigation"
  ```

---

## Task 12: Play Store Metadata

**Files:**
- Create: `fastlane/metadata/android/en-US/title.txt`
- Create: `fastlane/metadata/android/en-US/short_description.txt`
- Create: `fastlane/metadata/android/en-US/full_description.txt`
- Create: `fastlane/metadata/android/en-US/changelog.txt`

- [ ] **Step 1: Create metadata files**
  Create: `fastlane/metadata/android/en-US/title.txt`
  ```
  Dusk
  ```

  Create: `fastlane/metadata/android/en-US/short_description.txt`
  ```
  Everything. Everyone. Everywhere.
  ```

  Create: `fastlane/metadata/android/en-US/full_description.txt`
  ```
  Dusk is the social super-app that brings everything you love in one place.

  FEED & DISCOVERY
  - Curated "For You" feed with trending content
  - Real-time posts from people you follow
  - Trending hashtags and communities

  STORIES & REELS
  - Share ephemeral moments with Stories
  - Watch fullscreen Reels like TikTok
  - Go live and stream to your community

  MESSAGING
  - Direct messages with typing indicators
  - Group chats and voice messages
  - Read receipts and emoji reactions

  CREATORS
  - Subscribe to creators (Bronze/Silver/Gold tiers)
  - Exclusive content and community access
  - Revenue analytics and insights

  Join millions on Dusk — Everything. Everyone. Everywhere.
  ```

  Create: `fastlane/metadata/android/en-US/changelog.txt`
  ```
  - Improved feed performance with pagination
  - Optimistic UI for instant feedback
  - Enhanced error handling and retries
  - Deep link support for sharing
  ```

- [ ] **Step 2: Create Fastfile**
  Create: `fastlane/Fastfile`
  ```ruby
  default_platform(:android)

  platform :android do
    desc "Deploy to Google Play Store"
    lane :deploy do
      gradle(task: "bundleRelease")
      
      upload_to_play_store(
        track: 'production',
        release_status: 'draft',
        json_key: 'play-store-key.json'
      )
    end

    desc "Deploy to internal testing"
    lane :internal do
      gradle(task: "bundleRelease")
      
      upload_to_play_store(
        track: 'internal'
      )
    end
  end
  ```

- [ ] **Step 3: Commit**
  ```bash
  git add fastlane/
  git commit -m "chore(store): add Play Store metadata and Fastlane config"
  ```

---

## Task 13: Screenshot Automation

**Files:**
- Create: `.maestro/screenshots/feed.yml`
- Create: `.maestro/screenshots/profile.yml`
- Create: `.github/workflows/screenshots.yml`

- [ ] **Step 1: Create Maestro test flows**
  Create: `.maestro/screenshots/feed.yml`
  ```yaml
  appId: com.dusk.messenger
  ---
  - launchApp
  - waitForAnimationToEnd
  - tapOn: "For You"
  - scrollUntilVisible:
      element:
        id: "post-card-0"
  - takeScreenshot: 01_feed
  ```

  Create: `.maestro/screenshots/profile.yml`
  ```yaml
  appId: com.dusk.messenger
  ---
  - launchApp
  - waitForAnimationToEnd
  - tapOn: "Profile"
  - waitForAnimationToEnd
  - takeScreenshot: 05_profile
  ```

- [ ] **Step 2: Create screenshot workflow**
  Create: `.github/workflows/screenshots.yml`
  ```yaml
  name: Generate Screenshots

  on:
    push:
      branches: [main]
    workflow_dispatch:

  jobs:
    screenshots:
      runs-on: macos-latest
      steps:
        - uses: actions/checkout@v4

        - name: Setup Android SDK
          uses: android-actions/setup-android@v3

        - name: Create AVD and run tests
          uses: reactivecircus/android-emulator-runner@v2
          with:
            api-level: 34
            target: google_apis
            arch: x86_64
            script: |
              ./gradlew installRelease
              maestro test .maestro/screenshots/

        - name: Upload screenshots
          uses: actions/upload-artifact@v4
          with:
            name: screenshots
            path: ~/.maestro/screenshots/
  ```

- [ ] **Step 3: Commit**
  ```bash
  git add .maestro/ .github/workflows/screenshots.yml
  git commit -m "chore(screenshots): add Maestro automation for store screenshots"
  ```

---

## Summary

| Task | Description | Estimated Time |
|------|-------------|----------------|
| 1 | Install Sentry deps | 5 min |
| 2 | Sentry initialization | 10 min |
| 3 | User context | 10 min |
| 4 | Pagination utilities | 15 min |
| 5 | Paginated feed | 20 min |
| 6 | Optimistic mutation hook | 15 min |
| 7 | Optimistic like button | 15 min |
| 8 | Retry logic | 15 min |
| 9 | API client retry | 10 min |
| 10 | Deep link config | 15 min |
| 11 | Deep link handler | 10 min |
| 12 | Store metadata | 10 min |
| 13 | Screenshot automation | 15 min |

**Total estimated time:** ~2.5 hours

---

## Spec Self-Review

- [x] Coverage: All 6 gaps addressed with clear tasks
- [x] Placeholders: No TBD or TODO items
- [x] Paths: All file paths match Dusk structure
- [x] Commands: Exact commands with expected outputs
- [x] Types: Consistent with existing codebase
