package com.xd.waypoint.activity;

import static com.xd.waypoint.MApplication.getContext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.xd.waypoint.R;
import com.xd.waypoint.utils.PickUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;

public class FileActivity extends BaseActivity implements View.OnClickListener{
    private Button btnOpenFile;
    private List<Waypoint> pointList=new ArrayList<>();
    private float altitude=10.0f;//!!!
    public static void start(Context context){
        Intent intent=new Intent(context,FileActivity.class);
        context.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        context = this;
        // 定义要打开的文件路径
        btnOpenFile=(Button)findViewById(R.id.btn_openfile);
        btnOpenFile.setOnClickListener(this);

    }
    private String getMimeType(String filePath) {
        String mimeType;
        // 根据文件后缀名判断文件类型
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "txt":
                mimeType = "text/plain";
                break;
            case "pdf":
                mimeType = "application/pdf";
                break;
            // 添加更多文件类型的判断...
            default:
                mimeType = "*/*";
                break;
        }
        return mimeType;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/plain"); // 只显示 txt 文件
        // 添加这一句表示对目标应用临时授权该Uri所代表的文件（Android 7.0及以上版本必须！）
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);// 此条非必须
        startActivityForResult(intent, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            Uri selectedFileUri = data.getData();
          //  String filePath = selectedFileUri.getPath();
            String filePath =    PickUtils.getPath(getContext(),selectedFileUri);
            // 处理文件路径
            handleFilePath(filePath,selectedFileUri);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void handleFilePath(String filePath, Uri uri) {
        // 处理文件路径的逻辑
        File file = new File(filePath);
        if (file.exists()) {
            // 文件存在，进行进一步处理
            showToast("文件存在，请进行下一步处理");
           // String content= TXTReader.readTxtFile(filePath);
          //  String fileContent = readFileContent(file);
            // 处理文件内容
       //     processFileContent(fileContent);



            try (//FileInputStream fileInputStream = new FileInputStream(file);
                 FileInputStream fileInputStream=openFileInput("loglat.txt");
                 InputStreamReader inputReader = new InputStreamReader(fileInputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputReader);){


                String line="";

                while((line=bufferedReader.readLine())!=null){
                    String regex=",|，";//不区分中英分隔
                    String[] s=line.split(regex);
                    pointList.add(new Waypoint(Double.parseDouble(s[0].trim()),Double.parseDouble(s[1].trim()),Float.parseFloat(s[2].trim())));
                }
               showToast("存储完毕");


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            finally {
//                try{
//                fileInputStream.close();
//                inputReader.close();
//                bufferedReader.close();
//                } catch(IOException e){
//                   System.err.print("error close"+e.getMessage());
//                }
//            }

        } else {
            // 文件不存在，给出提示
            showToast("文件不存在");
        }
    }

    private void processFileContent(){

    }

    private void openFile(){
        String filePath = Environment.getExternalStorageDirectory() + "/deviceId.txt";

        // 创建一个File对象
        File file = new File(filePath);

        // 检查文件是否存在
        if(file.exists()) {
            // 获取文件的URI
            Uri uri = Uri.fromFile(file);

            // 创建打开文件的Intent
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // 设置Intent的数据类型为文件类型
            intent.setDataAndType(uri, getMimeType(filePath));

            // 添加Intent的标志，以确保文件由外部应用程序打开
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 启动Intent
            startActivity(intent);
        } else {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
        }
    }
    /*ActivityResultLauncher launcher = registerForActivityResult(new ResultContract(), new ActivityResultCallback<Intent>() {
        @Override
        public void onActivityResult(Intent result) {
            if (result == null){
                return;
            }
            Uri uri = result.getData();
            //文件路径
            String mFilePath = PickUtils.getPath(getContext(),uri);
            //文件名
            String mFileName =PickUtils.getFileName(getContext(),uri);
        }
    });*/


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_openfile:
                openFilePicker();
               // launcher.launch(true);
                break;
        }
    }
    /*class ResultContract extends ActivityResultContract<Boolean, Intent> {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, Boolean input) {
            Intent intent = new Intent();
            intent.setType("video/*;image/*");//同时选择视频和图片
            intent.setAction(Intent.ACTION_GET_CONTENT);
            //  Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            return intent;
        }
        @Override
        public Intent parseResult(int resultCode, @Nullable Intent intent) {
            return intent;
        }
    }
*/

}

