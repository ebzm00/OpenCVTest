package com.example.opencvtest;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.googlecode.tesseract.android.TessBaseAPI;


import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;


public class MainActivity extends AppCompatActivity {

    //storage 권한 처리에 필요한 변수
    private final String[] CAMERA = {android.Manifest.permission.CAMERA};
    private final String[] STORAGE = {android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int CAMERA_CODE = 98;
    private final int STORAGE_CODE = 99;


    TessBaseAPI mTess; //Tess API reference
    String datapath = "" ; //언어데이터가 있는 경로


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //데이터 경로
        datapath = getFilesDir() + "/tesseract/";

        //트레이닝데이터가 카피되어 있는지 체크
        checkFile(new File(datapath + "tessdata/"),"kor");
        checkFile(new File(datapath + "tessdata/"),"eng");


        //Tesseract API 언어 세팅
        String lang = "kor+eng";

        //OCR 세팅
        mTess = new TessBaseAPI();
        mTess.init(datapath, lang);

        TextView tv = findViewById(R.id.OCRTextView);
        tv.setMovementMethod(new ScrollingMovementMethod());

        //카메라
        ImageButton camera = findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CallCamera();
            }
        });

        //사진 저장
        ImageButton picture = findViewById(R.id.picture);
        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GetAlbum();

            }
        });
    }


    // 권한 요청 결과 처리 함수
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_CODE:
                for (int grant : grantResults) {
                    if(grant != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "카메라 권한을 승인해 주세요.", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case STORAGE_CODE:
                for (int grant : grantResults) {
                    if(grant != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "저장소 권한을 승인해 주세요.", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    //다른 권한등도 확인이 가능하도록
    public boolean checkPermission(String[] permissions, int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, type);
                    return false;
                }
            }
        }
        return true;
    }

    //카메라 촬영 - 권한 처리
    private void CallCamera() {
        if(checkPermission(CAMERA,CAMERA_CODE) && checkPermission(STORAGE,STORAGE_CODE)) {
            Intent itt = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(itt,CAMERA_CODE,null);
        }
    }
    // 사진 저장
    public Uri savefile(String fileName, String mimeType, Bitmap bitmap) {

        ContentValues CV = new ContentValues();

        // MediaStore 에  파일명, mimeType을 지정
        CV.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        CV.put(MediaStore.Images.Media.MIME_TYPE, mimeType);

        // 안정성 검사
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CV.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        // MediaStore 에 파일을 저장
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, CV);
        if (uri != null) {
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());

                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    CV.clear();
                    // IS_PENDING을 초기화
                    CV.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, CV, null, null);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    // 결과
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ImageView imageView = findViewById(R.id.avatars);



        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CAMERA_CODE:
                    if (data != null && data.getExtras() != null && data.getExtras().get("data") != null) {
                        Bitmap img = (Bitmap) data.getExtras().get("data");
                        Uri uri = savefile(RandomFileName(), "image/jpeg", img);
                        imageView.setImageURI(uri);
                    }
                    break;
                case STORAGE_CODE:
                    if (data != null && data.getData() != null) {
                        Uri uri = data.getData();
                        imageView.setImageURI(uri);
                    }
                    break;
            }
        }
        //문자인식 진행
        if(imageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            processImage(bitmap);

        }
    }

    // 파일명을 날짜로 저장
    public String RandomFileName() {
        String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis());
        return fileName;
    }

    // 갤러리 취득
    public void GetAlbum() {
        if (checkPermission(STORAGE, STORAGE_CODE)) {
            Intent itt = new Intent(Intent.ACTION_PICK);
            itt.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(itt, STORAGE_CODE);
        }
    }



    private void processImage(Bitmap bitmap){
            Toast.makeText(getApplicationContext(),"이미지가 복잡할 경우 해석 시 많은 시간이 소요될 수도 있습니다.", Toast.LENGTH_LONG).show();
            mTess.setImage(bitmap);
            String OCRresult = mTess.getUTF8Text();

            //인식된 문자열을 단어 단위로 분할하여 배열에 저장
            String[] words = OCRresult.split("[^a-zA-Z0-9']+");

            //단어들을 출력
            StringBuilder builder = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            builder.append((i + 1) + ". ");
            builder.append(words[i]);
            builder.append("\n");
        }


        TextView OCRTextView = findViewById(R.id.OCRTextView);
            OCRTextView.setText(builder.toString().trim());
    }

        private void copyFiles(String lang) {
            try {
                //location we want the file to be at
                String filepath = datapath + "/tessdata/" + lang + ".traineddata";

                //get access to AssetManager
                AssetManager assetManager = getAssets();

                //open byte streams for reading/writing
                InputStream inStream = assetManager.open("tessdata/" + lang + ".traineddata");
                OutputStream outStream = Files.newOutputStream(Paths.get(filepath));

                //copy the file to the location specified by filepath
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inStream.read(buffer)) != -1) {
                    outStream.write(buffer,0,read);
                }
                outStream.flush();
                outStream.close();
                inStream.close();

            }  catch (IOException e) {
                e.printStackTrace();
            }  catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        //파일 존재 확인
        private void checkFile(File dir,String lang) {
            //directory does not exist, but we can successfully create it
            if(!dir.exists()&&dir.mkdirs()) {
                copyFiles(lang);
            }
            //The directory exists, but there is no data file in it
            if(dir.exists()) {
                String datafilePath = datapath+"/tessdata/" + lang + ".traineddata";
                File datafile = new File(datafilePath);
                if (!datafile.exists()) {
                    copyFiles(lang);
                }
            }
        }





}