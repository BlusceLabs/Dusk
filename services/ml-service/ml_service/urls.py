from django.urls import path, include
from rest_framework.decorators import api_view
from rest_framework.response import Response

@api_view(["GET"])
def health(request):
    return Response({"status": "ok", "service": "dusk-ml", "version": "0.1.0"})

urlpatterns = [
    path("health", health),
    path("api/recommendations/", include("recommendations.urls")),
    path("api/trending/", include("trending.urls")),
    path("api/nsfw/", include("nsfw.urls")),
]
