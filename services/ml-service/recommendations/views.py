import os
import json
import random
import requests
from rest_framework.decorators import api_view
from rest_framework.response import Response

REDIS_URL = os.environ.get("UPSTASH_REDIS_REST_URL", "")
REDIS_TOKEN = os.environ.get("UPSTASH_REDIS_REST_TOKEN", "")

MOCK_TOPICS = [
    "photography", "music", "coding", "travel", "art",
    "gaming", "fashion", "food", "sports", "tech",
]

def redis_get(key: str):
    if not REDIS_URL or not REDIS_TOKEN:
        return None
    try:
        r = requests.get(
            f"{REDIS_URL}/get/{key}",
            headers={"Authorization": f"Bearer {REDIS_TOKEN}"},
            timeout=2,
        )
        data = r.json()
        return data.get("result")
    except Exception:
        return None

def compute_score(post: dict, user_interests: list[str]) -> float:
    score = 0.0
    content = (post.get("content") or "").lower()
    hashtags = post.get("hashtags") or []
    for interest in user_interests:
        if interest in content or interest in hashtags:
            score += 2.0
    score += min(post.get("likes", 0) / 1000.0, 5.0)
    score += min(post.get("comments", 0) / 500.0, 3.0)
    score += random.uniform(0, 0.5)
    return score

@api_view(["GET"])
def recommendations(request):
    user_id = request.query_params.get("user_id", "")
    limit = int(request.query_params.get("limit", 20))

    cached = redis_get(f"recs:{user_id}")
    if cached:
        try:
            return Response({"source": "cache", "recommendations": json.loads(cached)})
        except Exception:
            pass

    interests = MOCK_TOPICS[:random.randint(3, 6)]
    mock_posts = [
        {
            "post_id": f"post_{i}",
            "score": round(random.uniform(0.5, 10.0), 3),
            "reason": random.choice(["trending in your interests", "popular with similar users", "from someone you may know"]),
        }
        for i in range(limit)
    ]
    mock_posts.sort(key=lambda p: p["score"], reverse=True)

    return Response({
        "user_id": user_id,
        "interests": interests,
        "recommendations": mock_posts,
        "algorithm": "collaborative_filtering_v1",
    })

@api_view(["GET"])
def similar_posts(request, post_id: str):
    limit = int(request.query_params.get("limit", 10))
    similar = [
        {"post_id": f"similar_{post_id}_{i}", "similarity": round(random.uniform(0.5, 0.99), 3)}
        for i in range(limit)
    ]
    similar.sort(key=lambda p: p["similarity"], reverse=True)
    return Response({"post_id": post_id, "similar": similar})

@api_view(["GET"])
def user_suggestions(request):
    user_id = request.query_params.get("user_id", "")
    suggestions = [
        {
            "user_id": f"user_{i}",
            "username": f"creator_{random.randint(100, 999)}",
            "reason": random.choice(["follows people you follow", "popular in your area", "similar interests"]),
            "score": round(random.uniform(0.5, 1.0), 3),
        }
        for i in range(10)
    ]
    return Response({"user_id": user_id, "suggestions": suggestions})
