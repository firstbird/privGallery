package org.horaapps.leafpic.data;

import static android.media.ExifInterface.TAG_DATETIME;
import static android.media.ExifInterface.TAG_GPS_LATITUDE_REF;
import static android.media.ExifInterface.TAG_GPS_LONGITUDE_REF;
import static android.media.ExifInterface.TAG_GPS_TIMESTAMP;
import static android.media.ExifInterface.TAG_ISO;

import android.content.Context;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import org.horaapps.leafpic.data.provider.CPHelper;
import org.horaapps.leafpic.progress.ProgressException;
import org.horaapps.leafpic.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by dnld on 8/8/17.
 */

public class MediaHelper {

    private static Uri external = MediaStore.Files.getContentUri("external");

    public static Observable<Media> deleteMedia(Context context, Media media) {
        return Observable.create(subscriber -> {
            try {
                internalDeleteMedia(context, media);
                subscriber.onNext(media);
            } catch (ProgressException e) {
                subscriber.onError(e);
            }
            subscriber.onComplete();
        });
    }

    public static Observable<Media> deleteMediaPrivacy(Context context, Media media) {
        return Observable.create(subscriber -> {
            try {
                internalDeletePrivacy(context, media);
                subscriber.onNext(media);
            } catch (ProgressException e) {
                subscriber.onError(e);
            }
            subscriber.onComplete();
        });
    }

    public static Observable<Album> deleteAlbum(Context context, Album album) {
        return Observable.create(subscriber -> {

            ArrayList<Observable<Media>> sources = new ArrayList<>(album.getCount());

            CPHelper.getMedia(context, album)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            media -> sources.add(MediaHelper.deleteMedia(context.getApplicationContext(), media)),
                            subscriber::onError,
                            () -> Observable.mergeDelayError(sources)
                                    .observeOn(AndroidSchedulers.mainThread(), true)
                                    .subscribeOn(Schedulers.newThread())
                                    .subscribe(
                                            item -> {
                                            },
                                            subscriber::onError,
                                            () -> {
                                                subscriber.onNext(album);
                                                subscriber.onComplete();
                                            })
                    );
        });
    }

    public static boolean internalDeleteMedia(Context context, Media media) throws ProgressException {
        File file = new File(media.getPath());
        StorageHelper.deleteFile(context, file);
        context.getContentResolver().delete(external, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getPath()});
        return true;
    }

    public static boolean internalDeletePrivacy(Context context, Media media) throws ProgressException {
        File file = new File(media.getPath());
        try {
            ExifInterface exif = new ExifInterface(file.getPath());
//            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
//            String timeStamp = exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP);
//            String dataTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
//            String location = exif.getAttribute(ExifInterface.TAG_SUBJECT_LOCATION);
//            Log.v("mzl", "internalDeletePrivacy orientation: " + orientation + " timeStamp: " + timeStamp + " dataTime: " + dataTime + " location: " + location);
            Log.v("mzl", "internalDeletePrivacy, [info]: " + getInfo(file.getPath()));
            exif.setAttribute(TAG_GPS_LATITUDE_REF, null);
            exif.setAttribute(TAG_GPS_LONGITUDE_REF, null);
            exif.setAttribute(TAG_DATETIME, null);
            exif.setAttribute(TAG_GPS_TIMESTAMP, null);
            exif.setAttribute(TAG_ISO, "610");
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        StorageHelper.deleteFile(context, file);
//        context.getContentResolver().delete(external, MediaStore.MediaColumns.DATA + "=?", new String[]{file.getPath()});
        return true;
    }

    /**
     * @param path 图片路径
     */
    public static String getInfo(String path) {
        try {

            ExifInterface exifInterface = new ExifInterface(path);

            String guangquan = exifInterface.getAttribute(ExifInterface.TAG_APERTURE);
            String shijain = exifInterface.getAttribute(TAG_DATETIME);
            String baoguangshijian = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            String jiaoju = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
            String chang = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            String kuan = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            String moshi = exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            String zhizaoshang = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            String iso = exifInterface.getAttribute(TAG_ISO);
            String jiaodu = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION);
            String baiph = exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE);
            String altitude_ref = exifInterface.getAttribute(ExifInterface
                    .TAG_GPS_ALTITUDE_REF);
            String altitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
            String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latitude_ref = exifInterface.getAttribute(TAG_GPS_LATITUDE_REF);
            String longitude_ref = exifInterface.getAttribute(TAG_GPS_LONGITUDE_REF);
            String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String timestamp = exifInterface.getAttribute(TAG_GPS_TIMESTAMP);
            String processing_method = exifInterface.getAttribute(ExifInterface
                    .TAG_GPS_PROCESSING_METHOD);

            //转换经纬度格式
            double lat = score2dimensionality(latitude);
            double lon = score2dimensionality(longitude);

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("光圈 = " + guangquan+"\n")
                    .append("时间 = " + shijain+"\n")
                    .append("曝光时长 = " + baoguangshijian+"\n")
                    .append("焦距 = " + jiaoju+"\n")
                    .append("长 = " + chang+"\n")
                    .append("宽 = " + kuan+"\n")
                    .append("型号 = " + moshi+"\n")
                    .append("制造商 = " + zhizaoshang+"\n")
                    .append("ISO = " + iso+"\n")
                    .append("角度 = " + jiaodu+"\n")
                    .append("白平衡 = " + baiph+"\n")
                    .append("海拔高度 = " + altitude_ref+"\n")
                    .append("GPS参考高度 = " + altitude+"\n")
                    .append("GPS时间戳 = " + timestamp+"\n")
                    .append("GPS定位类型 = " + processing_method+"\n")
                    .append("GPS参考经度 = " + latitude_ref+"\n")
                    .append("GPS参考纬度 = " + longitude_ref+"\n")
                    .append("GPS经度 = " + lat+"\n")
                    .append("GPS纬度 = " + lon+"\n");

            //将获取的到的信息设置到TextView上
//            mText.setText(stringBuilder.toString());
            return stringBuilder.toString();

            /**
             * 将wgs坐标转换成百度坐标
             * 就可以用这个坐标通过百度SDK 去获取该经纬度的地址描述
             */
//            double[] wgs2bd = wgs2bd(lat, lon);


        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 将 112/1,58/1,390971/10000 格式的经纬度转换成 112.99434397362694格式
     * @param string 度分秒
     * @return 度
     */
    public static double score2dimensionality(String string) {
        double dimensionality = 0.0;
        if (null==string){
            return dimensionality;
        }

        //用 ，将数值分成3份
        String[] split = string.split(",");
        for (int i = 0; i < split.length; i++) {

            String[] s = split[i].split("/");
            //用112/1得到度分秒数值
            double v = Double.parseDouble(s[0]) / Double.parseDouble(s[1]);
            //将分秒分别除以60和3600得到度，并将度分秒相加
            dimensionality=dimensionality+v/Math.pow(60,i);
        }
        return dimensionality;
    }

    static double pi = 3.14159265358979324;
    static double a = 6378245.0;
    static double ee = 0.00669342162296594323;
    public final static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

    public static double[] wgs2bd(double lat, double lon) {
        double[] wgs2gcj = wgs2gcj(lat, lon);
        double[] gcj2bd = gcj2bd(wgs2gcj[0], wgs2gcj[1]);
        return gcj2bd;
    }

    public static double[] gcj2bd(double lat, double lon) {
        double x = lon, y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
        double bd_lon = z * Math.cos(theta) + 0.0065;
        double bd_lat = z * Math.sin(theta) + 0.006;
        return new double[] { bd_lat, bd_lon };
    }

    public static double[] bd2gcj(double lat, double lon) {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double gg_lon = z * Math.cos(theta);
        double gg_lat = z * Math.sin(theta);
        return new double[] { gg_lat, gg_lon };
    }

    public static double[] wgs2gcj(double lat, double lon) {
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = lat + dLat;
        double mgLon = lon + dLon;
        double[] loc = { mgLat, mgLon };
        return loc;
    }

    private static double transformLat(double lat, double lon) {
        double ret = -100.0 + 2.0 * lat + 3.0 * lon + 0.2 * lon * lon + 0.1 * lat * lon + 0.2 * Math.sqrt(Math.abs(lat));
        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lon * pi) + 40.0 * Math.sin(lon / 3.0 * pi)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lon / 12.0 * pi) + 320 * Math.sin(lon * pi  / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double lat, double lon) {
        double ret = 300.0 + lat + 2.0 * lon + 0.1 * lat * lat + 0.1 * lat * lon + 0.1 * Math.sqrt(Math.abs(lat));
        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * pi) + 40.0 * Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lat / 12.0 * pi) + 300.0 * Math.sin(lat / 30.0 * pi)) * 2.0 / 3.0;
        return ret;
    }

    public static boolean renameMedia(Context context, Media media, String newName) {
        // return if filename didn't change
        String oldFilename = media.getName();
        if (oldFilename.equals(newName))
            return true;

        boolean success = false;
        try {
            File from = new File(media.getPath());
            File to = new File(StringUtils.getPhotoPathRenamed(media.getPath(), newName));
            if (success = StorageHelper.moveFile(context, from, to)) {
                context.getContentResolver().delete(external,
                        MediaStore.MediaColumns.DATA + "=?", new String[]{from.getPath()});

                scanFile(context, new String[]{to.getAbsolutePath()});
                media.setPath(to.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static boolean moveMedia(Context context, Media media, String targetDir) {
        boolean success = false;
        try {
            File from = new File(media.getPath());
            File to = new File(targetDir, from.getName());
            if (success = StorageHelper.moveFile(context, from, to)) {

                context.getContentResolver().delete(external,
                        MediaStore.MediaColumns.DATA + "=?", new String[]{from.getPath()});


                scanFile(context, new String[]{StringUtils.getPhotoPathMoved(media.getPath(), targetDir)});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static boolean copyMedia(Context context, Media media, String targetDir) {
        boolean success = false;
        try {
            File from = new File(media.getPath());
            File to = new File(targetDir);
            if (success = StorageHelper.copyFile(context, from, to))
                scanFile(context, new String[]{StringUtils.getPhotoPathMoved(media.getPath(), targetDir)});

        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static void scanFile(Context context, String[] path) {
        MediaScannerConnection.scanFile(context.getApplicationContext(), path, null, null);
    }
}
