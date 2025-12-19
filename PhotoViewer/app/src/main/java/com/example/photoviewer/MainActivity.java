package com.example.photoviewer;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    ImageView imgView;
    TextView textView;
    //String site_url= "http://10.0.2.2:8000";
    String site_url= "https://inseop.pythonanywhere.com/";
    JSONObject post_json;
    String imageUrl= null;
    Bitmap bmImg= null;

    private static final int REQ_PICK = 1001;
    private Uri selectedImageUri = null;

    CloadImage taskDownload;
    PutPost taskUpload;
    private static final String PREFS = "app_prefs";
    private static final String KEY_START_SYNC = "start_sync_enabled";
    private CheckBox cbStartSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //imgView= (ImageView) findViewById(R.id.imgView);
        textView= (TextView)findViewById(R.id.textView);


        cbStartSync = findViewById(R.id.cb_start_sync);

        boolean startSync = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(KEY_START_SYNC, false);
        if (cbStartSync != null) cbStartSync.setChecked(startSync);

        // 시작 시 한 번만 자동 동기화
        if (startSync) {
            onClickDownload(null);
        }

        // 체크 변경시 저장
        if (cbStartSync != null) {
            cbStartSync.setOnCheckedChangeListener((btn, checked) -> {
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_START_SYNC, checked)
                        .apply();
                Toast.makeText(this,
                        checked ? "다음 시작 때 자동 동기화합니다" : "다음 시작 때 자동 동기화를 하지 않습니다",
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    public void onClickDownload(View v) {
        //...생략...
        if (taskDownload!= null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload= new CloadImage();

        taskDownload.execute(site_url+ "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    public void onClickUpload(View v) {
        //...여기에 코드 추가...
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_PICK);
        Toast.makeText(getApplicationContext(), "이미지를 선택하세요", Toast.LENGTH_SHORT).show();




        Toast.makeText(getApplicationContext(), "Upload", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri == null) {
                Toast.makeText(this, "이미지를 선택하지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2) 제목/본문 입력 다이얼로그 (간단히 코드로 구성)
            final EditText etTitle = new EditText(this);
            etTitle.setHint("제목");
            final EditText etText = new EditText(this);
            etText.setHint("본문");
            etText.setMinLines(3);
            etText.setGravity(android.view.Gravity.TOP);
            etText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (16 * getResources().getDisplayMetrics().density);
            container.setPadding(pad, pad, pad, pad);
            container.addView(etTitle,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            container.addView(etText,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            new android.app.AlertDialog.Builder(this)
                    .setTitle("이미지 게시")
                    .setView(container)
                    .setPositiveButton("업로드", (d, w) -> {
                        String title = etTitle.getText().toString().trim();
                        String text = etText.getText().toString().trim();
                        if (title.isEmpty() || text.isEmpty()) {
                            Toast.makeText(this, "제목과 본문을 입력하세요.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 3) PutPost 실행 (URL을 파라미터로 넘김)
                        taskUpload = new PutPost(title, text, selectedImageUri);
                        taskUpload.execute(site_url + "/api_root/Post/");
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        //...생략...
        @Override
        protected List<Bitmap> doInBackground(String... urls){
            List<Bitmap> bitmapList= new ArrayList<>();


        try{
            String apiUrl= urls[0];
            String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
            URL urlAPI= new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
            conn.setRequestProperty("Authorization", "Token " + token);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int responseCode= conn.getResponseCode();
            if (responseCode== HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                is.close();
                String strJson= result.toString();
                JSONArray aryJson= new JSONArray(strJson);

                //배열 내 모든 이미지 다운로드
                for(int i=0;i < aryJson.length();i++){
                    post_json= (JSONObject) aryJson.get(i);
                    imageUrl= post_json.getString("image");

                    if (!imageUrl.equals("")) {
                        URL myImageUrl= new URL(imageUrl);
                        conn = (HttpURLConnection) myImageUrl.openConnection();
                        InputStream imgStream= conn.getInputStream();

                        Bitmap imageBitmap= BitmapFactory.decodeStream(imgStream);
                        bitmapList.add(imageBitmap);
                        // 이미지 리스트에 추가
                        imgStream.close();
                    }
                }
            }
        }
        catch(IOException | JSONException e){
            e.printStackTrace();
        }
        return bitmapList;
    }
        @Override
        protected void onPostExecute(List<Bitmap> images){
            if (images.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            }
            else {
                textView.setText("이미지 로드 성공!");
                RecyclerView recyclerView= findViewById(R.id.recyclerView);
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                recyclerView.setAdapter(adapter);
            }
        }
    }


    private class PutPost extends AsyncTask<String, Void, Boolean> {
    //...여기에 코드 추가...
        private final String title;
        private final String text;
        private final Uri imageUri;
        private String errorMsg = null;

        PutPost(String title, String text, Uri imageUri) {
            this.title = title;
            this.text = text;
            this.imageUri = imageUri;
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(MainActivity.this, "업로드 중…", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            String apiUrl = urls[0];
            String boundary = "----AndroidFormBoundary" + System.currentTimeMillis();
            String LF = "\r\n";

            HttpURLConnection conn = null;
            InputStream in = null;

            try {
                URL url = new URL(apiUrl);
                String token = "bf46b8f9337d1d27b4ef2511514c798be1a954b8";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                sdf.setTimeZone(TimeZone.getDefault()); // KST면 +09:00
                String publishedIso = sdf.format(new Date());

                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                java.io.DataOutputStream out = new java.io.DataOutputStream(conn.getOutputStream());

                // text field: author
                out.writeBytes("--" + boundary + LF);
                out.writeBytes("Content-Disposition: form-data; name=\"author\"" + LF + LF);
                out.write("1".getBytes("UTF-8"));  // 예: user id=1
                out.writeBytes(LF);

                // text field: title
                out.writeBytes("--" + boundary + LF);
                out.writeBytes("Content-Disposition: form-data; name=\"title\"" + LF + LF);
                out.write(title.getBytes("UTF-8"));
                out.writeBytes(LF);

                // text field: text
                out.writeBytes("--" + boundary + LF);
                out.writeBytes("Content-Disposition: form-data; name=\"text\"" + LF + LF);
                out.write(text.getBytes("UTF-8"));
                out.writeBytes(LF);

                out.writeBytes("--" + boundary + LF);
                out.writeBytes("Content-Disposition: form-data; name=\"published_date\"" + LF + LF);
                out.write(publishedIso.getBytes("UTF-8"));
                out.writeBytes(LF);

                // file field: image
                String mime = getContentResolver().getType(imageUri);
                if (mime == null) mime = "image/jpeg";
                out.writeBytes("--" + boundary + LF);
                out.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"" + LF);
                out.writeBytes("Content-Type: " + mime + LF + LF);

                in = getContentResolver().openInputStream(imageUri);
                byte[] buf = new byte[8192];
                int n;
                while (in != null && (n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                out.writeBytes(LF);

                // end boundary
                out.writeBytes("--" + boundary + "--" + LF);
                out.flush();
                out.close();
                if (in != null) in.close();

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                    return true;
                } else {
                    InputStream err = conn.getErrorStream();
                    if (err != null) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(err));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        errorMsg = sb.toString();
                    } else {
                        errorMsg = "HTTP " + code;
                    }
                    return false;
                }

            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignore) {}
                if (conn != null) conn.disconnect();
            }
        }
        @Override
        protected void onPostExecute(Boolean ok) {
            if (ok) {
                Toast.makeText(MainActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();
                onClickDownload(null);
                selectedImageUri = null;
            } else {
                Toast.makeText(MainActivity.this, "업로드 실패: " + (errorMsg == null ? "" : errorMsg), Toast.LENGTH_LONG).show();
            }
        }
    }

}

