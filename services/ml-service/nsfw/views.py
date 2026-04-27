import re
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status

NSFW_PATTERNS = [
    r"\b(spam|scam|phishing|click.?here.?to.?win)\b",
    r"\b(hate|slur)\b",
]
SPAM_PATTERNS = [
    r"(follow\s+back|f4f|l4l)",
    r"(buy\s+followers|get\s+free\s+followers)",
    r"(dm\s+for\s+promo|promo\s+for\s+promo)",
]

def score_text(text: str) -> dict:
    text_lower = text.lower()
    nsfw_hits = sum(1 for p in NSFW_PATTERNS if re.search(p, text_lower))
    spam_hits = sum(1 for p in SPAM_PATTERNS if re.search(p, text_lower))
    caps_ratio = sum(1 for c in text if c.isupper()) / max(len(text), 1)
    nsfw_score = min(nsfw_hits * 0.4, 1.0)
    spam_score = min(spam_hits * 0.35 + (caps_ratio > 0.6) * 0.2, 1.0)
    return {
        "nsfw": round(nsfw_score, 3),
        "spam": round(spam_score, 3),
        "safe": nsfw_score < 0.5 and spam_score < 0.5,
    }

@api_view(["POST"])
def check_content(request):
    text = request.data.get("text", "")
    content_type = request.data.get("type", "text")
    if not text:
        return Response({"error": "text is required"}, status=status.HTTP_400_BAD_REQUEST)
    scores = score_text(text)
    return Response({
        "content_type": content_type,
        "scores": scores,
        "action": "block" if not scores["safe"] else "allow",
        "model": "dusk-content-classifier-v1",
    })

@api_view(["POST"])
def report_content(request):
    post_id = request.data.get("post_id")
    reason = request.data.get("reason", "unknown")
    reporter_id = request.data.get("reporter_id")
    if not post_id or not reporter_id:
        return Response({"error": "post_id and reporter_id required"}, status=status.HTTP_400_BAD_REQUEST)
    return Response({
        "reported": True,
        "post_id": post_id,
        "reason": reason,
        "status": "queued_for_review",
        "ticket_id": f"MOD-{abs(hash(post_id + reporter_id)) % 100000:05d}",
    })
