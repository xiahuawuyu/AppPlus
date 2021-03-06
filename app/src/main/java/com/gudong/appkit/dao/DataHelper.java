/*
 *     Copyright (c) 2015 GuDong
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in all
 *     copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *     SOFTWARE.
 */

package com.gudong.appkit.dao;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;

import com.gudong.appkit.App;
import com.gudong.appkit.utils.FileUtil;
import com.gudong.appkit.utils.RxUtil;
import com.gudong.appkit.utils.Utils;
import com.jaredrummler.android.processes.ProcessManager;
import com.jaredrummler.apkparser.ApkParser;
import com.jaredrummler.apkparser.model.ApkMeta;
import com.litesuits.orm.db.assit.QueryBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;

/**
 * Created by GuDong on 12/8/15 12:11.
 * Contact with 1252768410@qq.com.
 */
public class DataHelper {

   public static AppEntity checkAndSetAppEntityStatus(final AppEntity installedEntity){
        final AppEntity localResult = getAppByPackageName(installedEntity.getPackageName());
       //this app is a new app,now it not exist in my local db
       if(localResult ==null){
           installedEntity.setStatus(AppStatus.CREATE.ordinal());
           // the installed app info is change,so the
       } else if(!installedEntity.equals(localResult)){
           installedEntity.setStatus(AppStatus.CHANGE.ordinal());
       }else{
           installedEntity.setStatus(AppStatus.NORMAL.ordinal());
       }
       return installedEntity;
    }

    /**
     * query App info by local db
     * @param packageName Application's package name
     * @return return AppEntity if this package name is not exist db will return null
     */
    public static AppEntity getAppByPackageName(String packageName){
        if(TextUtils.isEmpty(packageName))return null;
        QueryBuilder queryBuilder = new QueryBuilder(AppEntity.class);
        queryBuilder = queryBuilder.whereEquals("packageName ", packageName);
        List<AppEntity>result = App.sDb.query(queryBuilder);
        try {
            return result.get(0);
        }catch (IndexOutOfBoundsException e){
            return null;
        }
    }

    public static Observable<List<AppEntity>>getAllEntityByDbAsyn(){
        return RxUtil.makeObservable(new Callable<List<AppEntity>>() {
            @Override
            public List<AppEntity> call() throws Exception {
                return App.sDb.query(AppEntity.class);
            }
        });
    }

    /**
     * get AppEntity for Application of AppPlus
     * @return AppEntity
     */
    public static AppEntity getAppPlusEntity(){
        return getAppByPackageName(App.sContext.getPackageName());
    }

    /**
     * get the running app list info
     * @param ctx
     * @return
     */
    public static Observable<List<AppEntity>> getRunningAppEntity(final Context ctx) {
        return RxUtil.makeObservable(new Callable<List<AppEntity>>() {
            @Override
            public List<AppEntity> call() throws Exception {
                List<ActivityManager.RunningAppProcessInfo> runningList = ProcessManager.getRunningAppProcessInfo(ctx);
                List<AppEntity> list = new ArrayList<>();
                for (ActivityManager.RunningAppProcessInfo processInfo : runningList) {
                    String packageName = processInfo.processName;
                    if (isNotShowSelf(ctx, packageName)) continue;
                    AppEntity entity = DataHelper.getAppByPackageName(packageName);
                    if (entity == null) continue;
                    list.add(entity);
                }
                return list;
            }
        });
    }

    public static Observable<List<AppEntity>>getExportedAppEntity(){
        return RxUtil.makeObservable(new Callable<List<AppEntity>>() {
            @Override
            public List<AppEntity> call() throws Exception {
                File parent = FileUtil.createDir(FileUtil.getSDPath(),FileUtil.KEY_EXPORT_DIR);
                File[]exportArray = parent.listFiles();
                List<AppEntity>exportList = new ArrayList<>();
                AppEntity entity = null;
                for(File file : exportArray){
                    ApkParser parser = ApkParser.create(file);
                    ApkMeta meta = parser.getApkMeta();
                    entity = new AppEntity();
                    entity.setAppName(meta.label);
                    entity.setAppIconData(parser.getIconFile().data);
                    entity.setVersionName(meta.versionName);
                    entity.setSrcPath(file.getAbsolutePath());
                    exportList.add(entity);
                }
                return exportList;
            }
        });
    }


    /**
     * check running list should show AppPlus or not
     * @param packagename
     * @return true if show else false
     */
    private static boolean isNotShowSelf(Context ctx, String packagename) {
        return !Utils.isShowSelf() && packagename.equals(ctx.getPackageName());
    }
}
