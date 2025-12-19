package com.example.photoviewer;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // 이 로그에 뜨는 긴 문자열이 '토큰'
        // 이걸 복사함.
        Log.d("FCM_TOKEN", "내 토큰: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // 앱이 켜져있을 때 알림 처리 (지금은 비워둬도 됨)
    }
}