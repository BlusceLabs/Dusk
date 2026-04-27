from django.urls import path
from . import views

urlpatterns = [
    path("check/", views.check_content),
    path("report/", views.report_content),
]
