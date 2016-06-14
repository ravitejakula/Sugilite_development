package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

public class ScriptDetailActivity extends AppCompatActivity {

    private LinearLayout operationStepList;
    private SugiliteData sugiliteData;
    private String scriptName;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_detail);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (savedInstanceState == null) {
            scriptName = this.getIntent().getStringExtra("scriptName");
        } else {
            scriptName = savedInstanceState.getString("scriptName");
        }
        operationStepList = (LinearLayout)findViewById(R.id.operationStepList);
        sugiliteData = (SugiliteData)getApplication();
        /* find the script with scriptName */
        if(((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName().contentEquals(scriptName)){
            for(SugiliteBlock block : traverseBlock((SugiliteStartingBlock)sugiliteData.getScriptHead())) {
                TextView operationStepItem = new TextView(this);
                operationStepItem.setText(block.getDescription() + "\n");
                operationStepList.addView(operationStepItem);
            }
        }
        else{
            //do nothing
        }
    }
    public void scriptDetailRunButtonOnClick (View view){
        new AlertDialog.Builder(this)
                .setTitle("Run Script")
                .setMessage("Are you sure you want to run this script?")
                .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //clear the queue first before adding new instructions
                        sugiliteData.clearInstructionQueue();
                        addItemsToInstructionQueue(traverseBlock((SugiliteStartingBlock)sugiliteData.getScriptHead()));
                        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                        prefEditor.putBoolean("recording_in_process", false);
                        prefEditor.commit();
                        //go to home screen for running the automation
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    public void scriptDetailCancelButtonOnClick (View view){
        finish();
    }
    public void scriptDetailDeleteButtonOnClick (View view){
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deleting")
                .setMessage("Are you sure you want to delete this script?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private List<SugiliteBlock> traverseBlock(SugiliteStartingBlock startingBlock){
        List<SugiliteBlock> sugiliteBlocks = new ArrayList<>();
        SugiliteBlock currentBlock = startingBlock;
        while(currentBlock != null){
            sugiliteBlocks.add(currentBlock);
            if(currentBlock instanceof SugiliteStartingBlock){
                currentBlock = ((SugiliteStartingBlock)currentBlock).getNextBlock();
            }
            else if (currentBlock instanceof SugiliteOperationBlock){
                currentBlock = ((SugiliteOperationBlock)currentBlock).getNextBlock();
            }
            else{
                currentBlock = null;
            }
        }
        return sugiliteBlocks;
    }

    private void addItemsToInstructionQueue(List<SugiliteBlock> blocks){
        for(SugiliteBlock block : blocks){
            sugiliteData.addInstruction(block);
        }
    }




}
