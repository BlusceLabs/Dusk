from django.urls import path
from . import views

urlpatterns = [
    path("", views.trending_topics),
    path("hashtags/", views.trending_hashtags),
]
