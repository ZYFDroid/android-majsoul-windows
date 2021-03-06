package com.example.gamebrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import wei.mark.standout.Utils;

import static android.view.View.GONE;

public class PluginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("插件列表");
        setContentView(R.layout.activity_plugin);
        new Thread(){
            @Override
            public void run() {
                Intent intent = new Intent(Intent.ACTION_MAIN,null);
                intent.addCategory("com.example.gamebrowser.ADDONS");
                List<ResolveInfo> mApps = getPackageManager().queryIntentActivities(intent, 0);
                apps.clear();
                for (ResolveInfo info : mApps) {
                    try {
                        PackageInfo pkg = getPackageManager().getPackageInfo(info.activityInfo.packageName,0);
                        String path = pkg.applicationInfo.sourceDir;
                        String version = pkg.versionName;
                        String desc = info.activityInfo.metaData.getString("gb-addons-desc","无描述");
                        String author = info.activityInfo.metaData.getString("gb-addons-author","未知");
                        apps.add(new AppInfo(info.activityInfo.loadIcon(getPackageManager()),info.activityInfo.loadLabel(getPackageManager()).toString(),path,desc,version,author,pkg.packageName));
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                }
                Collections.sort(apps,new Comparator<AppInfo>() {
                    @Override
                    public int compare(AppInfo o1, AppInfo o2) {
                        return o1.name.compareTo(o2.name);
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initList();
                    }
                });
            }
        }.start();

        findViewById(R.id.btnPlugin).setEnabled(Utils.getSP(this).getBoolean("accept_plugin2",false));
    }

    List<AppInfo> apps = new ArrayList<>();



    class AppListViewHolder
    {
        ImageView viewImg;
        TextView viewName;
        TextView viewDesc;
        TextView viewVer;
        TextView viewAuthor;
        CheckBox chkEnabled;
    }
    class AppInfo{
        Drawable bmp;String name;
        String apppath;
        String desc,version;
        String author;
        String pkgName;
        public AppInfo(Drawable bmp, String name,String apppath,String desc,String version,String author,String pkgName) {
            this.bmp = bmp;
            this.name = name;
            this.apppath = apppath;
            this.desc = desc;
            this.version = version;
            this.author = author;
            this.pkgName=pkgName;
        }
    }
    Set<String> enabledPlugin = new LinkedHashSet<>();
    void initList(){
        enabledPlugin.clear();
        Set<String> temp = Utils.getSP(this).getStringSet("enabled_plugin",null);
        if(null!=temp) {
            enabledPlugin.addAll(temp);
        }
        findViewById(R.id.proLoading).setVisibility(GONE);
        findViewById(R.id.listPlugin).setVisibility(View.VISIBLE);
        ((ListView)findViewById(R.id.listPlugin)).setAdapter(new AppAdapter());
    }

    void saveList(){
        Utils.getSP(this).edit().putStringSet("enabled_plugin",enabledPlugin).apply();
        unsave=true;
    }

    class AppAdapter extends BaseAdapter {
        public AppAdapter() {
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout layout;
            AppListViewHolder holder = new AppListViewHolder();
            if(convertView == null)
            {
                LayoutInflater inflater = getLayoutInflater();
                layout = (LinearLayout) inflater.inflate(R.layout.adapter_app, null);

                holder.viewImg =  layout.findViewById(R.id.viewImg);
                holder.viewName =  layout.findViewById(R.id.viewName);
                holder.viewDesc =  layout.findViewById(R.id.viewDesc);
                holder.viewVer =  layout.findViewById(R.id.viewVer);
                holder.viewAuthor =  layout.findViewById(R.id.viewAuthor);
                holder.chkEnabled=layout.findViewById(R.id.chkEnabled);
                layout.setTag(holder);
            }
            else
            {
                layout = (LinearLayout) convertView;
                holder = (AppListViewHolder) layout.getTag();
            }

            AppInfo info = (AppInfo) getItem(position);
            holder.viewImg.setImageDrawable(info.bmp);
            holder.viewName.setText(info.name);
            holder.viewVer.setText(info.version);
            holder.viewDesc.setText(info.desc);
            holder.viewAuthor.setText("作者："+info.author);
            holder.chkEnabled.setOnCheckedChangeListener(null);
            holder.chkEnabled.setChecked(enabledPlugin.contains(info.pkgName));
            holder.chkEnabled.setOnCheckedChangeListener(new PluginEnabler(info.pkgName));
            return layout;
        }

        class PluginEnabler implements CompoundButton.OnCheckedChangeListener{
            String pkgName;

            public PluginEnabler(String pkgName) {
                this.pkgName = pkgName;
            }

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(!enabledPlugin.contains(pkgName)){
                        enabledPlugin.add(pkgName);
                    }
                }
                else{
                    if(enabledPlugin.contains(pkgName)){
                        enabledPlugin.remove(pkgName);
                    }
                }
                saveList();
            }
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public Object getItem(int position)
        {
            return apps.get(position );
        }

        @Override
        public int getCount()
        {
            return apps.size();
        }
    }

    public void pluginhelp(View view) {
        String msg="插件说明：\n" +
                "\n" +
                "第一次使用，每次安装或卸载插件时都需要 载入插件一次\n" +
                "\n" +
                "注意：\n" +
                "由于插件的特殊性质，禁止滥用插件功能，包括但不限于开发，制作，分发，出售，购买，使用任何影响游戏平衡性，解锁付费内容等性质的插件，否则可能会导致账号被封，或者被追究法律责任。\n" +
                "解锁插件功能，即表示您已阅读并知晓以上内容，并愿意承担滥用插件功能造成的一切后果。\n" +
                "\n\n" +
                "警告：\n" +
                "请从信任的地方获取插件。一些木马程序可能会伪装成插件，请自行甄别。";
        AlertDialog ald =  new AlertDialog.Builder(this).setTitle("功能介绍").setMessage(msg).setPositiveButton("解锁插件功能", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                findViewById(R.id.btnPlugin).setEnabled(true);
                Utils.getSP(PluginActivity.this).edit().putBoolean("accept_plugin2",true).commit();
            }
        }).setNegativeButton(android.R.string.cancel,null).create();
        ald.show();
    }

    // Indicates changes of plugin list.
    boolean unsave=false;

    @SuppressWarnings("all")
    public void loadPlugin(View view) {
        new AsyncTask<List<AppInfo>,String,String>(){
            ProgressDialog pdd;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pdd = ProgressDialog.show(PluginActivity.this,"请稍后","正在载入插件...",true,false);
            }
            @Override
            protected void onProgressUpdate(String... values) {
                pdd.setMessage(values[0]);
            }
            @Override
            protected void onPostExecute(String s) {
                pdd.dismiss();
                Utils.showDialog(PluginActivity.this,s);
                unsave=false;
                super.onPostExecute(s);
            }


            @Override
            protected String doInBackground(List<AppInfo>... params) {
                StringBuilder result = new StringBuilder("插件加载：\r\n");
                publishProgress("正在清理插件缓存...");


                for (SettingActivity.GameEntry server :
                        SettingActivity.getGameList(PluginActivity.this)) {
                    try {
                        cleanDir(getPatchDir(server.uuid));
                        cleanDir(getModDir(server.uuid));
                    }catch (Exception ex){
                        return "插件缓存清理失败";
                    }
                }
                for (AppInfo info: params[0]) {
                    if(!enabledPlugin.contains(info.pkgName)){continue;}
                    publishProgress("正在加载 "+info.name);
                    try{
                        ZipFile zipf = new ZipFile(info.apppath);
                        ZipEntry indexEntry = zipf.getEntry("assets/index.json");
                        Log.w("Plugin Install","Reading index file...");
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        InputStream indexreader = zipf.getInputStream(indexEntry);
                        copyStream(indexreader,bos);
                        JSONArray jarr = new JSONArray(new String(bos.toByteArray(),"utf-8"));
                        indexreader.close();
                        bos.close();
                        Log.w("Plugin Install","Begining extract...");
                        for (int i = 0; i < jarr.length(); i++) {
                            JSONObject jobj = jarr.getJSONObject(i);
                            String sourceFile = jobj.getString("file");
                            String targetserver = jobj.getString("gameuid");
                            String filetype = jobj.getString("type");
                            String destPath = jobj.getString("path");
                            Log.w("Plugin Install","Extract:"+destPath);
                            ZipEntry src = zipf.getEntry("assets/"+sourceFile);
                            File fout =new File(getAddonRootDir(targetserver,filetype)+destPath);
                            if(!fout.getParentFile().exists()){fout.getParentFile().mkdirs();}
                            if(fout.exists()){fout.delete();}
                            fout.createNewFile();
                            InputStream zin = zipf.getInputStream(src);
                            FileOutputStream fos = new FileOutputStream(fout,false);
                            copyStream(zin,fos);
                            zin.close();
                            fos.close();
                        }
                        zipf.close();
                        result.append("加载 "+info.name+" 成功\r\n");
                    }catch (Exception ex){
                        ex.printStackTrace();
                        result.append("加载 "+info.name+" 失败:"+ex.getClass().getName()+":"+ex.getMessage()+"\r\n");
                    }
                }
                return result.toString();
            }
        }.execute(apps);
    }

    @Override
    public void onBackPressed() {
        if(unsave && findViewById(R.id.btnPlugin).isEnabled()){
            loadPlugin(null);
            return;
        }
        super.onBackPressed();
    }

    void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len=0;
        while ((len=in.read(buffer,0,buffer.length))>0){
            out.write(buffer,0,len);
        }
    }



    String getPatchDir(String cachepref) {
        String path = getFilesDir().getAbsolutePath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path +cachepref+ "/patch/";
    }
    String getModDir(String cachepref) {
        String path = getFilesDir().getAbsolutePath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path +cachepref+ "/mods/";
    }

    String getAddonRootDir(String cachepref,String type) {
        String path = getFilesDir().getAbsolutePath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        return path +cachepref+ "/"+type+"/";
    }

    public static void cleanDir(String dirPath){
        File file = new File(dirPath);
        if(!file.exists()){file.mkdirs();}
        File[] files = file.listFiles();
        if(files!=null){
            for (File f:
                 files) {
                deleteDir(f.getAbsolutePath());
            }
        }
    }



    public static void deleteDir(String dirPath)
    {
        Log.w("Plugin Cleanup","Delete: "+dirPath);
        File file = new File(dirPath);
        if(!file.exists()){return;}
        if(file.isFile())
        {
            file.delete();
        }else
        {
            File[] files = file.listFiles();
            if(files == null)
            {
                file.delete();
            }else
            {
                for (int i = 0; i < files.length; i++)
                {
                    deleteDir(files[i].getAbsolutePath());
                }
                file.delete();
            }
        }
    }
}
