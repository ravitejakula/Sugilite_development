package edu.cmu.hcii.sugilite.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

import edu.cmu.hcii.sugilite.MainActivity;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * @author toby
 * @date 6/20/16
 * @time 4:03 PM
 */
public class StatusIconManager {
    private ImageView statusIcon;
    private Context context;
    private WindowManager windowManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;
    private SugiliteScreenshotManager screenshotManager;

    public StatusIconManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.sugiliteScriptDao = new SugiliteScriptDao(context);
        this.serviceStatusManager = new ServiceStatusManager(context);
        this.screenshotManager = new SugiliteScreenshotManager(sharedPreferences, context);
    }

    /**
     * add the status icon using the context specified in the class
     */
    public void addStatusIcon(){
        statusIcon = new ImageView(context);
        statusIcon.setImageResource(R.mipmap.ic_launcher);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);


        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = 200;
        addCrumpledPaperOnTouchListener(statusIcon, params, displaymetrics, windowManager);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23){
            checkDrawOverlayPermission();
            if(Settings.canDrawOverlays(context))
                windowManager.addView(statusIcon, params);
        }
        else {
            windowManager.addView(statusIcon, params);
        }


    }

    /**
     * remove the status icon from the window manager
     */
    public void removeStatusIcon(){
        try{
            if(statusIcon != null)
                windowManager.removeView(statusIcon);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /** code to post/handler request for permission */
    public final static int REQUEST_CODE = -1010101;

    public void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23) {
            if (!Settings.canDrawOverlays(context)) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                /** request permission via start activity for result */
                context.startActivity(intent);

            }
        }
    }

    /**
     * make the chathead draggable. ref. http://blog.dision.co/2016/02/01/implement-floating-widget-like-facebook-chatheads/
     * @param view
     * @param mPaperParams
     * @param displayMetrics
     * @param windowManager
     */
    private void addCrumpledPaperOnTouchListener(final View view, final WindowManager.LayoutParams mPaperParams, DisplayMetrics displayMetrics, final WindowManager windowManager) {
        final int windowWidth = displayMetrics.widthPixels;
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            GestureDetector gestureDetector = new GestureDetector(context, new SingleTapUp());

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    // gesture is clicking -> pop up the on-click menu
                    AlertDialog.Builder textDialogBuilder = new AlertDialog.Builder(context);
                    final boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
                    textDialogBuilder.setTitle("STATUS: " + (recordingInProcess ? "RECORDING:" : "NOT RECORDING") + "\nChoose Operation:");
                    final SugiliteStartingBlock startingBlock = (SugiliteStartingBlock) sugiliteData.getScriptHead();
                    boolean recordingInProgress = sharedPreferences.getBoolean("recording_in_process", false);
                    String[] operations = {"View Current Script: " + (startingBlock == null ? "NULL" : startingBlock.getScriptName()), (recordingInProcess? "End Recording" : (startingBlock == null ? "New Recording" : "Resume Recording: " + startingBlock.getScriptName())), "Quit Sugilite"};
                    textDialogBuilder.setItems(operations, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                //bring the user to the script list activity
                                case 0:
                                    Intent intent = new Intent(context, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                    Toast.makeText(context, "view current script", Toast.LENGTH_SHORT).show();
                                    break;
                                case 1:
                                    if(recordingInProcess){
                                        //end recording
                                        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                                        prefEditor.putBoolean("recording_in_process", false);
                                        prefEditor.commit();
                                        Toast.makeText(context, "end recording", Toast.LENGTH_SHORT).show();
                                    }
                                    else{
                                        if(startingBlock == null) {
                                            //create a new script
                                            sugiliteData.clearInstructionQueue();
                                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                                            final EditText scriptName = new EditText(v.getContext());
                                            scriptName.setText("New Script");
                                            scriptName.setSelectAllOnFocus(true);
                                            builder.setMessage("Specify the name for your new script")
                                                    .setView(scriptName)
                                                    .setPositiveButton("Start Recording", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if(!serviceStatusManager.isRunning()){
                                                                //prompt the user if the accessiblity service is not active
                                                                AlertDialog.Builder builder1 = new AlertDialog.Builder(v.getContext());
                                                                builder1.setTitle("Service not running")
                                                                        .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                            @Override
                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                serviceStatusManager.promptEnabling();
                                                                                //do nothing
                                                                            }
                                                                        }).show();
                                                            }
                                                            else if (scriptName != null && scriptName.getText().toString().length() > 0) {
                                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                                editor.putString("scriptName", scriptName.getText().toString());
                                                                editor.putBoolean("recording_in_process", true);
                                                                editor.commit();
                                                                //set the active script to the newly created script
                                                                sugiliteData.initiateScript(scriptName.getText().toString() + ".SugiliteScript");
                                                                //save the newly created script to DB
                                                                try {
                                                                    sugiliteScriptDao.save((SugiliteStartingBlock)sugiliteData.getScriptHead());
                                                                }
                                                                catch (Exception e){
                                                                    e.printStackTrace();
                                                                }
                                                                Toast.makeText(v.getContext(), "Changed script name to " + sharedPreferences.getString("scriptName", "NULL"), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    })
                                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //do nothing
                                                        }
                                                    })
                                                    .setTitle("New Script");
                                            AlertDialog dialog2 = builder.create();
                                            dialog2.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                            dialog2.show();
                                        }
                                        else {
                                            //resume the recording of an existing script
                                            SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                                            prefEditor.putBoolean("recording_in_process", true);
                                            prefEditor.commit();
                                            Toast.makeText(context, "resume recording", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    break;
                                case 2:
                                    Toast.makeText(context, "quit sugilite", Toast.LENGTH_SHORT).show();
                                    try {
                                        screenshotManager.take(false);
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    });
                    Dialog dialog = textDialogBuilder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                    return true;

                }
                //gesture is not clicking - handle the drag & move
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mPaperParams.x;
                        initialY = mPaperParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // move paper ImageView
                        mPaperParams.x = initialX + (int) (initialTouchX - event.getRawX());
                        mPaperParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(view, mPaperParams);
                        return true;
                }
                return false;
            }

            class SingleTapUp extends GestureDetector.SimpleOnGestureListener {

                @Override
                public boolean onSingleTapUp(MotionEvent event) {
                    return true;
                }
            }

        });
    }





}