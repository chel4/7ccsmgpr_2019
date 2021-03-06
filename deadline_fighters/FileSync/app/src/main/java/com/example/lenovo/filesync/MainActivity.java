package com.example.lenovo.filesync;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentUris;
import android.database.Cursor;
import android.os.Build;
import android.os.FileObserver;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.database.sqlite.*;

import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONArray;





public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String bucketName = "deadlinefighters";
    private final String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FileSync";
    private final String folder = "FileSync";

    private static final int CHOOSING_FILE_REQUEST = 1234;
    private String address = "10.40.176.129";//static IP for Siva's machine
    private int port = 80;

    private TextView tvFileName;
    private ListView lvItemList;
    private TextView tvFileInfo;

    private Uri fileUri;
    private MainFileObserver mainFileObserver;


    String URL ="http://10.40.176.129:80";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvFileName = findViewById(R.id.tv_file_name);
        tvFileName.setText("");

        lvItemList = findViewById(R.id.lv_item_list);
        tvFileInfo = findViewById(R.id.tv_file_info);
        tvFileInfo.setText("");

        findViewById(R.id.btn_add_file).setOnClickListener(this);
        findViewById(R.id.btn_sync).setOnClickListener(this);
        findViewById(R.id.btn_server_files).setOnClickListener(this);
        findViewById(R.id.btn_local_file).setOnClickListener(this);

//        try {
//            Socket socket = new Socket(address,port);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        if(isPathExist(filepath)){
            mainFileObserver = new MainFileObserver(filepath);
            mainFileObserver.startWatching();
        }


    }

    public void connectHttp(){
        RequestQueue queue = Volley.newRequestQueue(this);

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Toast.makeText(getApplicationContext(), "Response is: "+ response.substring(0,500), Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                tvFileInfo.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void openDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(getApplicationContext(), R.layout.dialog_item_function, null);

        dialog.setView(dialogView);
        dialog.show();

        TextView tvFileSelect = dialogView.findViewById(R.id.tv_file_select);
        Button btnSyncFile = dialogView.findViewById(R.id.btn_sync_file);
        Button btnDeleteFile = dialogView.findViewById(R.id.btn_delete_file);

        String fileName = tvFileInfo.getText().toString();
        tvFileSelect.setText(fileName);
        File file = new File(filepath, "/" + fileName);

        btnSyncFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendUploadRequest();
            }
        });

        btnDeleteFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!file.exists()){
                    sendDeleteRequest();
                } else{
                    deleteLocalFile();
                }

            }
        });

    }

    private void uploadFile() {


    }

    private void uploadFiles(){
        List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String,Object>>();
        fileList = getLocalFiles(filepath);

        if(fileList!=null) {
            for (int i = 0; i < fileList.size(); ++i) {
                String fileName = fileList.get(i).get("fileName").toString();
                tvFileInfo.setText(fileName);
                uploadFile();
            }
        }
    }

    private void syncFiles(){
        uploadFiles();
        downloadFiles();
    }

    private void listServerFile(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = tvFileInfo.getText().toString();
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                JSONObject json = new JSONObject();
                try {
                    json.put("operation","listAll");
                    json.put("bucketName","deadlinefighters");
                    json.put("fileDetails",fileName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, json,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                if(response.has("listAll")) {
                                    Log.d("ListAll","ListAll obtained");
                                    try {
                                        JSONArray fileList = (JSONArray) response.get("listAll");
                                        Log.d("ListAll","ListAll is valid");

                                        List<HashMap<String, Object>> fileMapList = new ArrayList<HashMap<String, Object>>();


                                        for(int i=0; i < fileList.length() ; i++) {
                                            HashMap<String, Object> hashMap = new HashMap<String, Object>();
                                            JSONObject json_data = fileList.getJSONObject(i);
                                            Log.d("ListAll","File:"+String.valueOf(json_data.getString("fileName"))+"is processed");
                                            String fileName=json_data.getString("fileName");
                                            hashMap.put("seq",i+1);
                                            hashMap.put("fileName",fileName);
                                            fileMapList.add(hashMap);
                                        }

                                        SimpleAdapter simpleAdapter = new SimpleAdapter(
                                                getApplicationContext(),
                                                fileMapList,
                                                R.layout.spec_item_list,
                                                new String[]{"seq","fileName"},
                                                new int[]{R.id.tv_item_seq, R.id.tv_item_name}
                                        );
                                        lvItemList.setAdapter(simpleAdapter);
                                        setListViewHeightBasedOnChildren(lvItemList);

                                        final List<HashMap<String, Object>> finalFileList = fileMapList;
                                        lvItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                                final String fileName = finalFileList.get(i).get("fileName").toString();
                                                System.out.println(finalFileList.get(i));
                                                tvFileInfo.setText(fileName);
                                                openDialog();
                                            }
                                        });


                                    } catch (Exception e) {
                                        Toast.makeText(getApplicationContext(),"Invalid list obtained", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else {
                                    Toast.makeText(getApplicationContext(),"No file in server", Toast.LENGTH_SHORT).show();
                                }

                            }


                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // TODO: Handle error

                            }
                        }
                );
                queue.add(jsonObjectRequest);
            }
        }).start();

        /*
        List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String,Object>>();
        fileList = getFile();

        SimpleAdapter simpleAdapter = new SimpleAdapter(
                this,
                fileList,
                R.layout.spec_item_list,
                new String[]{"seq","filename"},
                new int[]{R.id.tv_item_seq, R.id.tv_item_name}
        );
        lvItemList.setAdapter(simpleAdapter);
        setListViewHeightBasedOnChildren(lvItemList);


        final List<HashMap<String, Object>> finalFileList = fileList;
        lvItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final String fileName = finalFileList.get(i).get("filename").toString();
                System.out.println(finalFileList.get(i));
                tvFileInfo.setText(fileName);
                openDialog();
            }
        }); */
    }

    private void listLocalFile(){
        List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String,Object>>();
        fileList = getLocalFiles(filepath);

        if(fileList!=null) {
            SimpleAdapter simpleAdapter = new SimpleAdapter(
                    this,
                    fileList,
                    R.layout.spec_item_list,
                    new String[]{"seq", "fileName"},
                    new int[]{R.id.tv_item_seq, R.id.tv_item_name}
            );
            lvItemList.setAdapter(simpleAdapter);
            setListViewHeightBasedOnChildren(lvItemList);


            final List<HashMap<String, Object>> finalFileList = fileList;
            lvItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    final String fileName = finalFileList.get(i).get("fileName").toString();
                    System.out.println(finalFileList.get(i));
                    tvFileInfo.setText(fileName);
                    openDialog();
                }
            });
        }
    }

    /**
     * Taken from Stack Overflow - https://stackoverflow.com/a/26501296
     * Updates the ListView height based on its children
     *
     * @param listView the ListView to adjust
     *
     * This is only for interface and I want to focus on the functionality.
     */

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); ++i) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private List<HashMap<String, Object>> getFile(){

        getFileCallable getFileCallable = new getFileCallable();
        FutureTask<List<HashMap<String, Object>>> result = new FutureTask<>(getFileCallable);
        new Thread(result).start();
        List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String,Object>>();
        try {
            fileList = result.get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return fileList;
    }

    protected void sendDownloadRequest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = tvFileInfo.getText().toString();
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                JSONObject json = new JSONObject();
                try {
                    json.put("operation","downloadFile");
                    json.put("bucketName","deadlinefighters");
                    json.put("fileDetails",fileName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, json,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {

                                try {
                                    if (response!=null) {

                                        String url = response.getString("url");
                                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                                                .setDestinationInExternalPublicDir(folder,fileName)
                                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                                        request.allowScanningByMediaScanner();

                                        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                                        manager.enqueue(request);
                                        Toast.makeText(getApplicationContext(), "Download complete.", Toast.LENGTH_LONG).show();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(getApplicationContext(), "String Response : "+ response.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(),"Error getting response", Toast.LENGTH_SHORT).show();
                    }
                });
                jsonObjectRequest.setTag("request for download.");
                queue.add(jsonObjectRequest);
            }
        }).start();


    }

    protected void sendUploadRequest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = tvFileInfo.getText().toString();
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                JSONObject json = new JSONObject();
                try {
                    json.put("operation","uploadFile");
                    json.put("bucketName","deadlinefighters");
                    json.put("fileDetails",fileName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, json,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String url = response.getString("url");
                                    Log.d("Upload",fileName +"   "+url);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            uploadFile(fileName,url);
                                        }
                                    }).start();
                                    Toast.makeText(getApplicationContext(),"Upload Completed", Toast.LENGTH_SHORT).show();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(),"Error getting response", Toast.LENGTH_SHORT).show();
                    }
                });
                jsonObjectRequest.setTag("Request for upload.");
                queue.add(jsonObjectRequest);
            }
        }).start();


    }

    public void uploadFile(String fileName, String Url) {
        
        String filePath = filepath + "/" + fileName;
        Log.d("uploadfile",filePath);

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(filePath);

        if (!sourceFile.isFile()) {
            Log.e("uploadFile", "Source File not exist.");
        }
        else
        {
            try {

                // open a URL connection
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                java.net.URL url = new URL(Url);


                // Open a HTTP  connection to  the URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", filePath);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
//
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";file\""
                        + fileName + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                int serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                e.printStackTrace();
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }

        }
}

    private void downloadFile() {

    }

    private void downloadFiles(){
        List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String,Object>>();
        fileList = getFile();

        for(int i=0; i<fileList.size(); ++i){
            final String fileName = fileList.get(i).get("filename").toString();
            tvFileInfo.setText(fileName);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadFile();
                }
            }).start();

        }
        Toast.makeText(getApplicationContext(), "Download Completed!" , Toast.LENGTH_SHORT).show();

    }

    protected void sendDeleteRequest(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String fileName = tvFileInfo.getText().toString();
                RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                JSONObject json = new JSONObject();
                try {
                    json.put("operation","deleteFile");
                    json.put("bucketName","deadlinefighters");
                    json.put("fileDetails",fileName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, json,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String url = response.getString("url");
                                    Log.d("delete",fileName +"   "+url);
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            deleteFile(fileName,url);
                                        }
                                    }).start();
                                    Toast.makeText(getApplicationContext(),"Request for delete sent", Toast.LENGTH_SHORT).show();

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(),"Error invoking delete", Toast.LENGTH_SHORT).show();
                    }
                });
                jsonObjectRequest.setTag("Request for delete.");
                queue.add(jsonObjectRequest);
            }
        }).start();


    }


    private void deleteFile(String fileName, String Url) {
        String filePath = filepath + "/" + fileName;
        Log.d("deleteFile called for: ", filePath);
        RequestQueue queue = Volley.newRequestQueue(this);

        try {

            StringRequest stringRequest = new StringRequest(Request.Method.DELETE, Url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Toast.makeText(getApplicationContext(),"Deletion successful", Toast.LENGTH_SHORT).show();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(),"Error during deleting file", Toast.LENGTH_SHORT).show();
                }
            });
            queue.add(stringRequest);
        }
        catch(Exception e){
            Log.d("DeleteFile","Unknown error: "+e);
        }
    }

    public List<HashMap<String, Object>> getLocalFiles(String dirPath) {
        File file = new File(dirPath);
        int seq = 0;
        if(!file.exists()){
            return null;
        }else{

            File[] files = file.listFiles();

            if(files==null){
                return null;
            }

            List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String, Object>>();
            for (File f : files) {
                if(f.isFile()){
                    String filename=f.getName();
                    Log.d("LOGCAT","fileName:"+filename);
                    java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String fileLastModified = simpleDateFormat.format(f.lastModified()) ;
                    Log.d("LOGCAT","lastModified:"+fileLastModified);

                    try {
                        ++seq;
                        HashMap<String, Object> hashMap = new HashMap<String, Object>();
                        hashMap.put("seq", seq);
                        hashMap.put("fileName", filename);
                        hashMap.put("LastModified", fileLastModified);
                        fileList.add(hashMap);

                        System.out.println(hashMap);

                    }catch (Exception e){
                    }
                } else if(f.isDirectory()){
                    getLocalFiles(f.getAbsolutePath());
                }
            }
            return fileList;
        }
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();

        if (i == R.id.btn_add_file) {
            showChoosingFile();
        }  else if (i == R.id.btn_sync) {
//            sendUploadRequest();
            sendDownloadRequest();
//            connectHttp();
//            downloadFile();
        } else if (i == R.id.btn_server_files){
            listServerFile();
        } else if (i == R.id.btn_local_file){
            listLocalFile();
        }
    }

    private void deleteLocalFile(){

        final String fileName = tvFileInfo.getText().toString();
        final String filePath = filepath + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            file.delete();
        }
    }

    private void showChoosingFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), CHOOSING_FILE_REQUEST);
        Log.d("MainActivity", "Start Activity.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CHOOSING_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            Log.d("MainActivity", "Get file uri: " + fileUri);
            if(fileUri != null){
                String fileName = getFileName(getApplicationContext(),fileUri);

                Log.d("MainActivity", "Get File name: " + fileName);
                if (fileName != null){
                    tvFileInfo.setText(fileName);
                    File file = new File(filepath, "/" + fileName);
                    createFile(getApplicationContext(), fileUri, file);
                    Toast.makeText(getApplicationContext(), "Add file Completed!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getApplicationContext(), "Add file failed!", Toast.LENGTH_SHORT).show();
                }

            }

        }

    }

    private void createFile(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFileName( final Context context, final Uri uri ) {
        final String scheme = uri.getScheme();
        String fileName = "";
        String data = null;
        if ( scheme == null )
            data = uri.getPath();

        // Pick from uri type is file
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();

         //Pick from uri type is content
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {

            if(DocumentsContract.isDocumentUri(context, uri)){
                String documentId = DocumentsContract.getDocumentId(uri);

                // Image pick from recent or images
                if ("com.android.providers.media.documents".equals(uri.getAuthority())) {

                    // Split at colon, use second item in the array
                    String id = DocumentsContract.getDocumentId(uri).split(":")[1];

                    // where id is equal to
                    String selection = MediaStore.Images.Media._ID + "=?";

                    String[] selectionArgs = {id};
                    data = getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);

                    // Pick other document or image from downloads
                }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                    data = getDataColumn(context, contentUri, null, null);
                }else if("com.android.externalstorage.documents".equals(uri.getAuthority())){
                    fileName = documentId.substring(documentId.lastIndexOf("/") + 1, documentId.length());
                }else{
                    return null;
                }

            }

            else {
                // image pick from gallery or photo
                data = getDataColumn(context, uri, null, null);
            }

        }
        if(data!= null){
            fileName = data.substring(data.lastIndexOf("/") + 1, data.length());
        }

        return fileName;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String path = null;

        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    private boolean isPathExist(final String filepath){
        File file = new File(filepath);

        if (!file.exists())
        {
            file.mkdirs();
            if (file.mkdirs())
            {
                return true;
            }
            else
                return false;
        }
        return true;
    }

    public class getFileCallable implements Callable<List<HashMap<String, Object>>> {
        @Override
        public List<HashMap<String, Object>> call() throws Exception {
            List<HashMap<String, Object>> fileList = new ArrayList<HashMap<String,Object>>();
            int seq = 0;

            /*ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(bucketName);
            ObjectListing objectListing;
            do {
                objectListing = s3Client.listObjects(listObjectsRequest);


                List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
                for (S3ObjectSummary objectSummary : summaries) {

                    ++seq;
                    HashMap<String, Object> hashMap = new HashMap<String, Object>();
                    hashMap.put("seq", seq);
                    hashMap.put("filename", objectSummary.getKey());
                    hashMap.put("ETag", objectSummary.getETag());
                    hashMap.put("size", objectSummary.getSize());
                    hashMap.put("LastModified", objectSummary.getLastModified());
                    fileList.add(hashMap);

                    System.out.println("filename:"+ objectSummary.getKey()+" fileLastModified:"+objectSummary.getLastModified());

                }
                listObjectsRequest.setMarker(objectListing.getNextMarker());
                return fileList;
            } while (objectListing.isTruncated());*/
            return null;
        }
    }

    public class MainFileObserver extends FileObserver{
        String absolutePath;
        static final String TAG ="FileObserver";

        public MainFileObserver(String dir) {
            super(dir,FileObserver.ALL_EVENTS);
            absolutePath = dir;
        }

        @Override
        public void onEvent(int event, String path) {
            Log.d(TAG, "Entering FileWatcher for "+path);
            switch (event){
                case FileObserver.MODIFY:
                    Log.d(TAG, "MODIFY:"  + absolutePath +"/" + path);
                    break;
                case FileObserver.CREATE:
                    Log.d(TAG, "CREATE:"  + absolutePath +"/" + path);
                    break;
                case FileObserver.DELETE:
                    Log.d(TAG, "DELETE:"  + absolutePath +"/" + path);
                    sendDeleteRequest();
                    break;
            }
        }
    }



    }

