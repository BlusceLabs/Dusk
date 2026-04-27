# Firestore Security Rules

Copy these rules into your Firebase Console → Firestore Database → Rules tab.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // ─── helpers ─────────────────────────────────────────────────────────────
    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }

    function isValidString(s, maxLen) {
      return s is string && s.size() > 0 && s.size() <= maxLen;
    }

    // ─── users ───────────────────────────────────────────────────────────────
    match /users/{userId} {
      allow read: if isSignedIn();
      allow create: if isOwner(userId);
      allow update: if isOwner(userId) && (
        // Only allow safe profile fields to be updated
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['bio', 'displayName', 'username', 'avatar', 'banner',
                    'website', 'location', 'updatedAt', 'isPremium',
                    'premiumExpiresAt', 'fcmToken'])
      );
      allow delete: if false;
    }

    // ─── posts ────────────────────────────────────────────────────────────────
    match /posts/{postId} {
      allow read: if isSignedIn();
      allow create: if isOwner(request.resource.data.authorId)
        && isValidString(request.resource.data.content, 2000);
      allow update: if isSignedIn() && (
        // Only counters and like/bookmark arrays can be updated by others
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['likes', 'comments', 'reposts', 'views',
                    'likedBy', 'repostedBy', 'bookmarkedBy'])
      );
      allow delete: if isOwner(resource.data.authorId);

      // ── comments ────────────────────────────────────────────────────────────
      match /comments/{commentId} {
        allow read: if isSignedIn();
        allow create: if isOwner(request.resource.data.authorId)
          && isValidString(request.resource.data.content, 1000);
        allow update: if isSignedIn() && (
          request.resource.data.diff(resource.data).affectedKeys()
            .hasOnly(['likes', 'likedBy'])
        );
        allow delete: if isOwner(resource.data.authorId);
      }
    }

    // ─── stories ─────────────────────────────────────────────────────────────
    match /stories/{userId} {
      allow read: if isSignedIn();
      allow write: if isOwner(userId);

      match /frames/{frameId} {
        allow read: if isSignedIn();
        allow create: if isOwner(userId);
        allow delete: if isOwner(userId);
      }
    }

    // ─── notifications ───────────────────────────────────────────────────────
    match /notifications/{notificationId} {
      allow read: if isSignedIn()
        && resource.data.recipientId == request.auth.uid;
      allow create: if isSignedIn();
      allow update: if isSignedIn()
        && resource.data.recipientId == request.auth.uid
        && request.resource.data.diff(resource.data).affectedKeys()
            .hasOnly(['isRead']);
      allow delete: if isSignedIn()
        && resource.data.recipientId == request.auth.uid;
    }

    // ─── communities ─────────────────────────────────────────────────────────
    match /communities/{communityId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn();
      allow update: if isSignedIn() && (
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['members', 'posts'])
      );
      allow delete: if false;

      match /members/{userId} {
        allow read: if isSignedIn();
        allow write: if isOwner(userId);
      }
    }

    // ─── streams ─────────────────────────────────────────────────────────────
    match /streams/{streamId} {
      allow read: if isSignedIn();
      allow create: if isOwner(request.resource.data.streamerId);
      allow update: if isOwner(resource.data.streamerId) || (
        // Anyone can update viewer count
        isSignedIn() &&
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['viewers'])
      );
      allow delete: if isOwner(resource.data.streamerId);
    }

    // ─── creators ────────────────────────────────────────────────────────────
    match /creators/{userId} {
      allow read: if isSignedIn();
      allow write: if isOwner(userId);

      match /tiers/{tierId} {
        allow read: if isSignedIn();
        allow write: if isOwner(userId);
      }

      match /activity/{activityId} {
        allow read: if isOwner(userId);
        allow create: if isSignedIn();
        allow update, delete: if false;
      }
    }

    // ─── deny everything else ────────────────────────────────────────────────
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## How to apply

1. Go to [Firebase Console](https://console.firebase.google.com) → your `duskapp26` project
2. Click **Firestore Database** in the left sidebar
3. Click the **Rules** tab at the top
4. Replace all existing rules with the rules above
5. Click **Publish**

## What these rules do

| Collection | Read | Write |
|---|---|---|
| `users/{uid}` | Any authenticated user | Only that user (safe fields only) |
| `posts/{id}` | Any authenticated user | Author can CRUD; others can update counters only |
| `posts/{id}/comments` | Any authenticated user | Author can CRUD; others can like only |
| `stories/{uid}/frames` | Any authenticated user | Only that user |
| `notifications/{id}` | Only recipient | Anyone can create; recipient can mark read/delete |
| `communities/{id}` | Any authenticated user | Members sub-collection: own user only |
| `streams/{id}` | Any authenticated user | Streamer or viewer count update |
| `creators/{uid}` | Any authenticated user | Only that user |
