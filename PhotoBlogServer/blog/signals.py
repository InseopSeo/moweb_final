from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import Post
import firebase_admin
from firebase_admin import credentials, messaging
import os

# ==========================================
# [설정 1] 서비스 계정 키 파일 경로

CRED_PATH = "C:/Users/seois/OneDrive/바탕 화면/moweb_final/PhotoBlogServer/fcm_key.json"

# 안드로이드 폰의 FCM 토큰

TARGET_TOKEN = "dEEsh8l4Sxu9lxeyBD7Bmj:APA91bHzW9KbOkD2WOJjShbx8Z22ZI82IK_PnxBaPeZYAtS9-81onYnf117trphueQzVqAXz3cLSs-PR2Q4gxoWmdJ_AO0MimDA2WMScc_UjUcU_3CDjSIQ"
# ==========================================

# Firebase 앱 초기화 (서버 켜질 때 한 번만 실행되도록 체크)
if not firebase_admin._apps:
    try:
        cred = credentials.Certificate(CRED_PATH)
        firebase_admin.initialize_app(cred)
        print("✅ Firebase 앱 초기화 완료!")
    except Exception as e:
        print(f"❌ Firebase 초기화 실패: {e}")

@receiver(post_save, sender=Post)
def send_fcm_notification(sender, instance, created, **kwargs):
    """
    게시물이 저장(save)될 때마다 실행되는 함수
    """
    # 1. '새로 생성된 글(created)'이고 + '이미지(image)'가 감지된 글인 경우에만 알림
    if created and instance.image:
        print(f"🚀 [알림 트리거] 새 상자 게시물 감지됨! (ID: {instance.id})")
        
        try:
            # 2. 메시지 구성
            message = messaging.Message(
                notification=messaging.Notification(
                    title='📦 택배 도착!',
                    body=f'새로운 택배가 현관에 감지되었습니다.\n({instance.created_date.strftime("%H:%M")})',
                ),
                token=TARGET_TOKEN, # 받는 사람
            )
            
            # 3. 전송
            response = messaging.send(message)
            print(f"✅ 알림 전송 성공: {response}")
            
        except Exception as e:
            print(f"⚠️ 알림 전송 실패: {e}")