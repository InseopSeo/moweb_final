# send_alert.py
# 알림 예시 코드

import firebase_admin
from firebase_admin import credentials, messaging

# 1. 키 파일 이름
cred = credentials.Certificate("PhotoBlogServer/fcm_key.json")
firebase_admin.initialize_app(cred)

# 2. 토큰
registration_token = 'dEEsh8l4Sxu9lxeyBD7Bmj:APA91bHzW9KbOkD2WOJjShbx8Z22ZI82IK_PnxBaPeZYAtS9-81onYnf117trphueQzVqAXz3cLSs-PR2Q4gxoWmdJ_AO0MimDA2WMScc_UjUcU_3CDjSIQ'

# 3. 메시지 구성
message = messaging.Message(
    notification=messaging.Notification(
        title='📦 택배 도착!',
        body='현관에 상자가 감지되었습니다.',
    ),
    token=registration_token,
)

# 4. 전송
response = messaging.send(message)
print('성공적으로 보냄:', response)