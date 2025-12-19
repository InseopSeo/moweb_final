import os
import cv2
import pathlib
import requests
import torch
import time
from datetime import datetime
import sys

class BoxUploader:
    # ==========================================
    # [설정] 서버 주소와 모델 경로를 확인하세요
    HOST = 'http://127.0.0.1:8000' # 로컬 테스트용
    # HOST = 'http://inseop.pythonanywhere.com' # 실제 배포용
    
    MODEL_PATH = r'C:/Users/seois/OneDrive/바탕 화면/moweb_final/yolov5/best.pt'
    SAVE_DIR = 'detected_images'
    # ==========================================

    def __init__(self):
        print("--- 시스템 초기화 및 로그인 ---")
        self.username = "seois" 
        self.password = input("Enter your password: ") # 보안 이슈로 가림.
        
        self.token = ''
        self.author_id = 1 # 글쓴이 ID (필요시 수정)
        self.prev_box_detected = False # 이전에 상자가 있었는지 여부 (0 or 1)

        # 1. 서버 로그인 및 토큰 발급
        self._login()

        # 2. YOLO 모델 로드
        print("⏳ YOLOv5 모델 로딩 중...")
        try:
            YOLO_DIR = r'C:/Users/seois/OneDrive/바탕 화면/moweb_final/yolov5'
            self.model = torch.hub.load(YOLO_DIR, 'custom', path=self.MODEL_PATH, source='local')
            self.model.conf = 0.35 # 확신도 35% 이상만
            print("✅ 모델 로드 완료!")
        except Exception as e:
            print(f"❌ 모델 로드 실패: {e}")
            sys.exit(1)

    def _login(self):
        try:
            # api-token-auth 주소는 drf 설정에 따라 다를 수 있음 (/api-token-auth/ 또는 /api-auth/ 등)
            res = requests.post(self.HOST + '/api-token-auth/', {
                'username': self.username,
                'password': self.password
            })
            res.raise_for_status() # 200 OK가 아니면 에러 발생
            self.token = res.json()['token']
            print(f"✅ 로그인 성공! (Token 획득 완료)")
        except Exception as e:
            print(f"❌ 로그인 실패: {e}")
            print("서버가 켜져 있는지, 아이디/비번이 맞는지 확인하세요.")
            sys.exit(1)

    def process_frame(self, frame):
        """프레임을 받아 추론하고, 변화가 생기면 업로드"""
        
        # 추론 실행
        results = self.model(frame)
        
        # 결과에서 'box'가 있는지 확인
        df = results.pandas().xyxy[0]
        detected_classes = df['name'].values.tolist()
        
        is_box_currently_present = 'box' in detected_classes

        # 상태 변화 감지 (Detection Change)
        # 이전에는 없었는데(False) -> 지금 생겼다면(True) : 업로드 실행
        if not self.prev_box_detected and is_box_currently_present:
            print("📦 상자가 새로 감지되었습니다! 업로드 시작...")
            self.send(frame)
        
        # 상태 업데이트 (현재 상태를 과거 상태로 저장)
        self.prev_box_detected = is_box_currently_present
        
        return results.render()[0] # 박스가 그려진 이미지 반환

    def send(self, image):
        now = datetime.now()
        
        # 1. 이미지 로컬 저장 (pathlib 사용)
        today = datetime.now()
        save_path = pathlib.Path(os.getcwd()) / self.SAVE_DIR / str(today.year) / str(today.month) / str(today.day)
        save_path.mkdir(parents=True, exist_ok=True)
        
        file_name = f"{today.hour}-{today.minute}-{today.second}-{today.microsecond}.jpg"
        full_path = save_path / file_name
        
        # 이미지 리사이즈 (용량 절약 및 전송 속도 향상)
        dst = cv2.resize(image, dsize=(640, 480), interpolation=cv2.INTER_AREA)
        cv2.imwrite(str(full_path), dst)
        
        # 2. 서버 전송
        # 주의: Django 설정에 따라 헤더 접두사가 'JWT' 일 수도 있고 'Token' 일 수도 있습니다.
        # 보내주신 파일에는 'JWT'로 되어 있어 그대로 유지합니다.
        headers = {'Authorization': 'JWT ' + self.token} 
        
        data = {
            'title': f'상자 도착! ({today.strftime("%H:%M:%S")})',
            'text': '택배 상자가 감지되었습니다.',
            'created_date': now,
            'published_date': now,
            'author': str(self.author_id),
            # 'is_box': 'True'
        }
        
        try:
            with open(full_path, 'rb') as f:
                files = {'image': f}
                res = requests.post(self.HOST + '/api_root/Post/', data=data, files=files, headers=headers)
                
            if res.status_code == 201:
                print(f"🚀 서버 업로드 성공! [{res.status_code}]")
            else:
                print(f"⚠️ 업로드 실패: {res.status_code} {res.text}")
                
        except Exception as e:
            print(f"❌ 전송 중 에러 발생: {e}")

# --- 메인 실행부 ---
if __name__ == "__main__":
    # 클래스 인스턴스 생성 (로그인 및 모델 로드 수행)
    uploader = BoxUploader()
    
    # 웹캠 사용
    #cap = cv2.VideoCapture(0)

    #print("모니터링 시작... (종료하려면 'q'를 누르세요)")
    

    # 동영상 파일 사용
    video_path = "C:/Users/seois/OneDrive/바탕 화면/moweb_final/yolov5/test_box.mp4"
    cap = cv2.VideoCapture(video_path)
    print("동영상 모니터링 시작... (종료하려면 'q'를 누르세요)")

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # 프레임 처리 (추론 및 업로드 판단)
        result_frame = uploader.process_frame(frame)

        # 화면 출력
        cv2.imshow('YOLOv5 Change Detection', result_frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()