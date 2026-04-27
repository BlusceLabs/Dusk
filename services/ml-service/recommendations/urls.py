from django.urls import path
from . import views

urlpatterns = [
    path("", views.recommendations),
    path("similar/<str:post_id>/", views.similar_posts),
    path("users/", views.user_suggestions),
]
