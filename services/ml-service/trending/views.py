import os
import json
import requests
from rest_framework.decorators import api_view
from rest_framework.response import Response

REDIS_URL = os.environ.get("UPSTASH_REDIS_REST_URL", "")
REDIS_TOKEN = os.environ.get("UPSTASH_REDIS_REST_TOKEN", "")

FALLBACK_HASHTAGS = [
    {"tag": "DuskVibes", "posts": 48200, "growth": "+12%"},
    {"tag": "GoldenHour", "posts": 31500, "growth": "+8%"},
    {"tag": "NeonAesthetic", "posts": 22100, "growth": "+24%"},
    {"tag": "MidnightCoding", "posts": 18900, "growth": "+5%"},
    {"tag": "CreatorEconomy", "posts": 15600, "growth": "+31%"},
    {"tag": "LoFiBeats", "posts": 12300, "growth": "+7%"},
    {"tag": "DigitalArt", "posts": 9800, "growth": "+18%"},
    {"tag": "OpenSource", "posts": 8400, "growth": "+3%"},
    {"tag": "StreamerLife", "posts": 7200, "growth": "+15%"},
    {"tag": "DarkMode", "posts": 6100, "growth": "+2%"},
]

def redis_zrange(key: str, count: int = 10):
    if not REDIS_URL or not REDIS_TOKEN:
        return []
    try:
        r = requests.get(
            f"{REDIS_URL}/zrange/{key}/0/{count - 1}/rev/withscores",
            headers={"Authorization": f"Bearer {REDIS_TOKEN}"},
            timeout=2,
        )
        result = r.json().get("result", [])
        pairs = []
        for i in range(0, len(result), 2):
            pairs.append({"tag": result[i], "score": float(result[i + 1])})
        return pairs
    except Exception:
        return []

@api_view(["GET"])
def trending_hashtags(request):
    limit = int(request.query_params.get("limit", 10))
    live = redis_zrange("dusk:trending:hashtags", limit)
    if live:
        return Response({"source": "live", "hashtags": live[:limit]})
    return Response({"source": "fallback", "hashtags": FALLBACK_HASHTAGS[:limit]})

@api_view(["GET"])
def trending_topics(request):
    limit = int(request.query_params.get("limit", 10))
    return Response({
        "trending": FALLBACK_HASHTAGS[:limit],
        "categories": ["Music", "Tech", "Art", "Travel", "Gaming"],
    })
