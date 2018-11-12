package com.example.arruda.reccoon3d;

import android.content.Intent;
import android.graphics.Bitmap;


import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.text.SimpleDateFormat;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.kosalgeek.android.photoutil.ImageBase64;
import com.kosalgeek.android.photoutil.ImageLoader;
import com.kosalgeek.genasync12.AsyncResponse;
import com.kosalgeek.genasync12.EachExceptionsHandler;
import com.kosalgeek.genasync12.PostResponseAsyncTask;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    //variaveis da interface grafica
    ImageView imageView; // campo onde sera exibido a foto
    Button btnCameraCalibrate;
    Button btnCalibrate;

    //variaveis para transferencia de dados
    private String ipAdress = "192.168.1.16";
    private static Socket newSocket;
    private static ServerSocket serverSocket;
    public static Buffer buffer;
    private static PrintWriter printWriter;
    public static BufferedWriter bufferedWriter;
    private static String directoryAppPath = Environment.DIRECTORY_DCIM+"/Reccoon3D";
    private final String TAG = this.getClass().getName();
    String imageDataString;



    String message ="";
    private File arquivo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        btnCameraCalibrate = (Button) findViewById(R.id.btnPicturesCalibration);
        btnCalibrate = (Button) findViewById((R.id.btnCalibrate));

        btnCalibrate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                try{
                    File file  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+"/Reccoon3D/123.jpg");
                    String filename = file.getAbsolutePath();
                    Bitmap bitmap = ImageLoader.init().from(filename).requestSize(1024,1024).getBitmap();
                    String encodedImage = ImageBase64.encode(bitmap);
                    Log.d(TAG,encodedImage);


                    HashMap<String,String> postData = new HashMap<String, String>();
                    postData.put("image",encodedImage);


                    PostResponseAsyncTask task = new PostResponseAsyncTask(MainActivity.this, postData, new AsyncResponse() {
                        @Override
                        public void processFinish(String s) {
                            if (s.contains("uploaded_success")){
                                Toast.makeText(getApplicationContext(),"Image Uploaded",Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(getApplicationContext(),"Error while uploading",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    task.execute("http://192.168.1.16/reccoon/upload/upload.php");
                    task.setEachExceptionsHandler(new EachExceptionsHandler() {
                        @Override
                        public void handleIOException(IOException e) {
                            Toast.makeText(getApplicationContext(),"Cannot Connect To Server",Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void handleMalformedURLException(MalformedURLException e) {
                            Toast.makeText(getApplicationContext(),"URL Error",Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void handleProtocolException(ProtocolException e) {
                            Toast.makeText(getApplicationContext(),"Protocol Error",Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void handleUnsupportedEncodingException(UnsupportedEncodingException e) {
                            Toast.makeText(getApplicationContext(),"Encoding Error",Toast.LENGTH_SHORT).show();
                        }
                    });

                }catch (FileNotFoundException e ){
                    Toast.makeText(getApplicationContext(),"Something Wrong Happened",Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCameraCalibrate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                //cria pasta para guardar fotos
                File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+"/Reccoon3D");
                if(!pictureDirectory.exists()){
                    pictureDirectory.mkdirs();
                }
                //nomeia a foto baseada em data e hora
                String pictureName = getCalibrationPictureName();
                File imagemCalib = new File(pictureDirectory,pictureName);
                Uri pictureUri = Uri.fromFile(imagemCalib);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,pictureUri);


                startActivityForResult(intent,0);

            }
        });

    }

    private String getCalibrationPictureName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());

        return "Calibration"+timestamp+".jpg";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

       super.onActivityResult(requestCode,resultCode,data);
        //Bitmap bitmap = (Bitmap)data.getExtras().get("data");
        //imageView.setImageBitmap(bitmap);
    }

    public void sendImage(View view){
        myTask mytask = new myTask();
        mytask.execute();
        Toast.makeText(getApplicationContext(),"Data Sent", Toast.LENGTH_LONG).show();
    }
    //Function that encodes the Bitmapfile into string
    public String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] arr=baos.toByteArray();
        String result=Base64.encodeToString(arr, Base64.DEFAULT);
        return result;
    }

    class myTask extends AsyncTask<Void,Void,Void>
    {
        //Faz a conexao entre aparelho e servidor
        File file  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+"/Reccoon3D/123.jpg");
        byte[] bytes = new byte[(int) file.length()];
        BufferedInputStream bis;
        @Override
        protected Void doInBackground(Void... params){

            try
            {
                //create socket and buffer to send the string message
                //newSocket = new Socket(ipAdress,5000);
                //printWriter = new PrintWriter(newSocket.getOutputStream());

                //bufferedWriter = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream()));

                //Reads the internal storage image and converts into string using Base64
                //bis=new BufferedInputStream(new FileInputStream(file));
                //bis.read(bytes,0,bytes.length);

                //ObjectOutputStream oos = new ObjectOutputStream(newSocket.getOutputStream());
                //oos.write(bytes);
                //oos.flush();



                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                message = ImageBase64.encode(bitmap); //encodes the bitmap

                HashMap<String, String> postData = new HashMap<String,String>();
                postData.put("image",message);

                PostResponseAsyncTask task = new PostResponseAsyncTask(MainActivity.this, postData, new AsyncResponse() {
                    @Override
                    public void processFinish(String s) {
                        if (s.contains("upload successful")){
                            Toast.makeText(getApplicationContext(),"Image Uploaded",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(),"Error while uploading",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                task.execute("http://192.168.1.16/reccoon/upload/upload.php");
                task.setEachExceptionsHandler(new EachExceptionsHandler() {
                    @Override
                    public void handleIOException(IOException e) {
                        Toast.makeText(getApplicationContext(),"Cannot Connect To Server",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void handleMalformedURLException(MalformedURLException e) {
                        Toast.makeText(getApplicationContext(),"URL Error",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void handleProtocolException(ProtocolException e) {
                        Toast.makeText(getApplicationContext(),"Protocol Error",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void handleUnsupportedEncodingException(UnsupportedEncodingException e) {
                        Toast.makeText(getApplicationContext(),"Encoding Error",Toast.LENGTH_SHORT).show();
                    }
                });


                //sends the enconded image
                //bufferedWriter.write(message);
                //bufferedWriter.flush();
                //bufferedWriter.close();
                newSocket.close();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }
    }



}
