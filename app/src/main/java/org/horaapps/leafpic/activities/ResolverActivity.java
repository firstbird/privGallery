package org.horaapps.leafpic.activities;

import static org.horaapps.leafpic.activities.SingleMediaActivity.EXTRA_ARGS_ALBUM;
import static org.horaapps.leafpic.activities.SingleMediaActivity.EXTRA_ARGS_MEDIA;
import static org.horaapps.leafpic.activities.SingleMediaActivity.EXTRA_ARGS_POSITION;
import static org.horaapps.leafpic.data.MediaHelper.score2dimensionality;

import static java.security.AccessController.getContext;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import org.horaapps.leafpic.R;
import org.horaapps.leafpic.adapters.AlbumsAdapter;
import org.horaapps.leafpic.adapters.MediaPagerAdapter;
import org.horaapps.leafpic.data.Album;
import org.horaapps.leafpic.data.AlbumSettings;
import org.horaapps.leafpic.data.Media;
import org.horaapps.leafpic.data.MediaHelper;
import org.horaapps.leafpic.fragments.BaseMediaFragment;
import org.horaapps.leafpic.interfaces.MediaClickListener;
import org.horaapps.leafpic.items.ActionsListener;
import org.horaapps.leafpic.progress.ProgressBottomSheet;
import org.horaapps.leafpic.util.AlertDialogsHelper;
import org.horaapps.leafpic.util.AnimationUtils;
import org.horaapps.leafpic.util.Measure;
import org.horaapps.leafpic.util.MediaUtils;
import org.horaapps.leafpic.util.Security;
import org.horaapps.leafpic.util.preferences.Prefs;
import org.horaapps.leafpic.views.GridSpacingItemDecoration;
import org.horaapps.liz.ThemedActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.LandingAnimator;

public class ResolverActivity extends ThemedActivity implements BaseMediaFragment.MediaTapListener {

    public static final String EXTRA_ARGS_SELECTED = "args_selected";
    @BindView(R.id.resolver_choose_pics) ViewPager mChoosePicsPager;
    @BindView(R.id.resolver_choose_num) TextView mChooseNumText;
    // todo 动态修改字样
    @BindView(R.id.resolver_image_info) TextView mImageInfo;
    @BindView(R.id.resolver_privacy_info) TextView mPrivacyInfoText;
    @BindView(R.id.resolver_sharePanel) RecyclerView mSharePanel;
    @BindView(R.id.share_prv_photos) Button mShareBtn;
//    private int mChooseNum;
    private boolean mIsPrivacyOn;
    private ArrayList<Media> mMedia;
    private MediaPagerAdapter mViewPagerAdapter;
    private AlbumsAdapter adapter;
    private GridSpacingItemDecoration spacingDecoration;
    private ActionsListener mActionsListener;
    private RecyclerView.LayoutManager mGridLayoutManager;
    private Album mAlbum;
    private int mViewPagerPosition;
    private Set<Integer> mSelected = new HashSet<>();

    private boolean needDeleteLocation;
    private boolean needDeletePhotoInfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resolver);
        ButterKnife.bind(this);

        loadAlbum(getIntent());
        setUpViews();
    }

    private void setUpViews() {
        initHeader();
        initChoosePics();
        initSharePanel();
    }

//    private void setUpColumns() {
//        int columnsCount = 2;
//
//        if (columnsCount != ((GridLayoutManager) mSharePanel.getLayoutManager()).getSpanCount()) {
//            mSharePanel.removeItemDecoration(spacingDecoration);
//            spacingDecoration = new GridSpacingItemDecoration(columnsCount, Measure.pxToDp(3, getBaseContext()), true);
//            mSharePanel.addItemDecoration(spacingDecoration);
//            mSharePanel.setLayoutManager(new GridLayoutManager(getBaseContext(), columnsCount));
//        }
//    }

    private void initSharePanel() {
        int spanCount = 2;
        spacingDecoration = new GridSpacingItemDecoration(spanCount, Measure.pxToDp(3, getBaseContext()), true);
        mSharePanel.setHasFixedSize(true);
        mSharePanel.addItemDecoration(spacingDecoration);
        mGridLayoutManager = new GridLayoutManager(getBaseContext(), spanCount);
        mSharePanel.setLayoutManager(mGridLayoutManager);
        mSharePanel.setLayoutDirection(View.SCROLL_AXIS_HORIZONTAL);
        if(Prefs.animationsEnabled()) {
            mSharePanel.setItemAnimator(
                    AnimationUtils.getItemAnimator(
                            new LandingAnimator(new OvershootInterpolator(1f))
                    ));
        }

        adapter = new AlbumsAdapter(getBaseContext(), mActionsListener);

        mSharePanel.setAdapter(adapter);
        return;

    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private void loadAlbum(Intent intent) {
        mAlbum = intent.getParcelableExtra(EXTRA_ARGS_ALBUM);
        mViewPagerPosition = intent.getIntExtra(EXTRA_ARGS_POSITION, 0);
        mSelected.addAll(intent.getParcelableArrayListExtra(EXTRA_ARGS_SELECTED));
        mMedia = intent.getParcelableArrayListExtra(EXTRA_ARGS_MEDIA);
    }

    private void initChoosePics() {
        mViewPagerAdapter = new MediaPagerAdapter(getSupportFragmentManager(), mMedia);
        mChoosePicsPager.setAdapter(mViewPagerAdapter);
        mChoosePicsPager.setCurrentItem(mViewPagerPosition);
        mChoosePicsPager.setOffscreenPageLimit(3);
    }

    private void initHeader() {
        mChooseNumText.setText(getChooseNumText());
        mPrivacyInfoText.setText(mIsPrivacyOn ? "隐私保护已开启" : "开启隐私保护");
        for (Integer i : mSelected) {
            Media media = mMedia.get(i);
            ExifInterface exifInterface = null;
            try {
                exifInterface = new ExifInterface(media.getPath());
                String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                if (latitude != null) {
                    double lat = score2dimensionality(latitude);
                    double lon = score2dimensionality(longitude);
                    lat = extracted(lat);
                    lon = extracted(lon);

                    String dataTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                    mImageInfo.setText(dataTime + " 经度: " + lat + " 纬度: " + lon);
                    break;
                }
                //转换经纬度格式
//                double lat = score2dimensionality(latitude);
//                double lon = score2dimensionality(longitude);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private double extracted(double d) {
        String f = Double.toString(d);
        BigDecimal bigDecimal = new BigDecimal(f);
        return bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private String getChooseNumText() {
        return "已选择 " + mSelected.size() + " 张图片";
    }

    @Override
    public void onViewTapped() {
        mViewPagerPosition = mChoosePicsPager.getCurrentItem();
        Log.v("mzl", "on viewTapped, position: " + mViewPagerPosition);
        if (mSelected.contains(mViewPagerPosition)) {
            mSelected.remove(mViewPagerPosition);
        } else {
            mSelected.add(mViewPagerPosition);
        }
        initHeader();
    }

    public void onPrivacyTextClick(View view) {
        AlertDialog dialog = AlertDialogsHelper.getCheckboxDialog(this, "清除位置信息", "清除拍摄数据",
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        needDeleteLocation = b;
                    }
                },
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        needDeletePhotoInfo = b;
                    }
                });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, this.getString(R.string.cancel).toUpperCase(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.dismiss();
                needDeleteLocation = false;
                needDeletePhotoInfo = false;
                Log.v("mzl", "resolver dialog dismissed");
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, this.getString(R.string.confirm_password).toUpperCase(), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
//                dialog.dismiss();
                Log.v("mzl", "resolver dialog confirmed");
            }
        });
        dialog.show();
    }

    public void onShareButtonClicked(View view) {
        List<Media> selectedToRmPrivacy = new ArrayList<>();
        for (Integer i : mSelected) {
            selectedToRmPrivacy.add(mMedia.get(i));
        }
        showDeleteBottomSheet(selectedToRmPrivacy);
        MediaUtils.shareMedia(this, selectedToRmPrivacy);
    }

    private void showDeleteBottomSheet(List<Media> selectedToRmPrivacy) {
        MediaUtils.deletePrivacy(this, selectedToRmPrivacy, getSupportFragmentManager(),
                new ProgressBottomSheet.Listener<Media>() {
                    @Override
                    public void onCompleted() {
//                        adapter.invalidateSelectedCount();
                        Log.v("mzl", "show dialog completed");
                    }

                    @Override
                    public void onProgress(Media item) {
                        int index = mMedia.indexOf(item);
                        Log.v("mzl", "onProgress item: " + item.getName() + " index: " + index);
                        if (index != -1) {
                            mSelected.remove(index);
                        }
                    }
                });
    }

    // 这种click回调其实是RV的adpater触发的
//    @Override
//    public void onMediaClick(Album album, ArrayList<Media> media, int position) {
//        Log.v("mzl", "album: " + album.getName() + " media size: " + media.size() + " position: " + position);
//    }
}