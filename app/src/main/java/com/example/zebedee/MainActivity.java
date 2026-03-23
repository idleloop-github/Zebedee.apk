package com.example.zebedee;

import java.util.Date;

//import android.R.layout;//
//A//import android.app.Activity;
import android.support.v4.app.FragmentActivity; //A//
import android.app.Dialog;
//import android.app.FragmentManager;
//import android.support.v4.app.FragmentManager; //A//
//A//import android.app.DialogFragment;
import android.support.v4.app.DialogFragment; //A//
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
//import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.view.View;		// FOCUS_DOWN
import android.widget.ScrollView;
import android.widget.Toast;
import android.app.AlertDialog;

//A//public class MainActivity extends Activity {
public class MainActivity extends FragmentActivity { //A//

	TextView textView_filename;
	TextView textView_log;
	ScrollView scrollView_log;
	
	String parametersFile=Environment.getExternalStorageDirectory().toString()+"/zebedee_parameters.txt";
	
	public String strLog;

	boolean bChkLogOff=false;
	
	Zebedee zebedee_thread;
	
	private final int REQUEST_CODE_PICK_DIR = 1;
	private final int REQUEST_CODE_PICK_FILE = 2;
	
	//A//final Activity activityForButton = this;
	final FragmentActivity activityForButton = this; //A//
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textView_filename = (TextView)findViewById(R.id.TextView_filename);
		textView_filename.setText(parametersFile, TextView.BufferType.EDITABLE);
		textView_log = (TextView)findViewById(R.id.log_text);
		//textView_log.setMovementMethod(new ScrollingMovementMethod());
    	scrollView_log = (ScrollView)findViewById(R.id.log_scroller);
    	strLog=getResources().getString(R.string.initiallogtext);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void activateZebedee(View v){
		if (zebedee_thread == null) {
			//zebedee_thread = new Zebedee(parametersFile, textView_log);
			strLog=strLog + "\n\nZebedee started at " + new Date() + "\n";
			showLog();
			zebedee_thread = new Zebedee(parametersFile, this);
		} else {
			exitZebedee();
		}
	}
	
	private void exitZebedee(){
		//textView_log.append( "zebedee stopped at " + new Date() );
		zebedee_thread = null;
		this.finish();
		// and, in order to close ports and whatever:
		// http://www.anddev.org/how_to_stop_an_activity-t5307.html
		// After this is called, your app process is no longer available in DDMS
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	public void setLogString(String strLog){
		if ( bChkLogOff == false ) {
			this.strLog=this.strLog + strLog;
		}
		//this.strLog=strLog;
	}
	
	public void showLog(){
		
		textView_log.setText(strLog);
		//textView_log.append(strLog);

	    // http://stackoverflow.com/questions/3506696/auto-scrolling-textview-in-android-to-bring-text-into-view
        //scroll chat all the way to the bottom of the text
        //HOWEVER, this won't scroll all the way down !!!
        //scrollView_log.fullScroll(View.FOCUS_DOWN);

        //INSTEAD, scroll all the way down with:
    	scrollView_log.post(new Runnable()
        {
            public void run()
            {
            	scrollView_log.fullScroll(View.FOCUS_DOWN);
            }
        });
	}
	
	// toggle show/hide logs
	public void chkLogOff_toggle(View v) {
		if ( bChkLogOff == false) {
			bChkLogOff=true;
			this.strLog=this.strLog + "\nLogs deactivated.\n\n";
		} else {
			bChkLogOff=false;
			this.strLog=this.strLog + "\nLogs activated.\n\n";
		}
		showLog();
	}
		
	public void onAboutItemClick(MenuItem item){
		
		// show about content on log window, just in case v<2.2 => About is not shown (?)
		strLog = strLog + getResources().getString(R.string.initiallogtext);
		showLog();
		
		AboutDialogFragment aboutDialog=new AboutDialogFragment(); 
		//A//aboutDialog.show(getFragmentManager(), "aboutDialog");
		// http://stackoverflow.com/questions/13175713/i-am-getting-an-error-the-method-showfragmentmanager-string
		aboutDialog.show(getSupportFragmentManager(), "aboutDialog"); //A//
	}

	// http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
	public static class AboutDialogFragment extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        // Get the layout inflater
	        LayoutInflater inflater = getActivity().getLayoutInflater();
	        // Inflate and set the layout for the dialog
	        // Pass null as the parent view because its going in the dialog layout
	        View viewInflated = inflater.inflate(R.layout.about_dialog, null);
	        builder.setView(viewInflated)
	        		.setPositiveButton(R.string.about_description_button, 
	            		   new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // code
	                   }
	        		});
	        // Create the AlertDialog object (and later return it)
	        AlertDialog dialog = builder.create();
	        // Accommodate text as html enriched content:
	        // http://stackoverflow.com/questions/5755541/cant-get-href-to-work-in-android-strings-xml
	        TextView about=(TextView)viewInflated.findViewById(R.id.TextView_about_description);
	        about.setMovementMethod(LinkMovementMethod.getInstance());
			about.setText(Html.fromHtml(getString(R.string.about_description)));
	        return dialog;
	    }
	}
	
	public void onExitItemClick(MenuItem item){
		exitZebedee();
	}

	// ------------------------
	// file browser integration (beginning)
	// ------------------------
	public void selectParametersFile(View v){
		   Intent fileExploreIntent = new Intent(
				    ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
				    null,
				        activityForButton,
				        ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.class
				    );
				//  fileExploreIntent.putExtra(
		   		//				      ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.startDirectoryParameter, 
		   		//				      "/sdcard"
				//  );//Here you can add optional start directory parameter, and file browser will start from that directory.
				    startActivityForResult(
				        fileExploreIntent,
				        REQUEST_CODE_PICK_FILE
				    );
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // TODO Auto-generated method stub
	    if (requestCode == REQUEST_CODE_PICK_FILE) {
	            if(resultCode == RESULT_OK) {
	                String newFile = data.getStringExtra(
	                        ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.returnFileParameter);
	                Toast.makeText(
	                    this, 
	                    "Received file name from file browser:"+newFile, 
	                    Toast.LENGTH_LONG
	                ).show(); 
	                // set file name:
	                parametersFile = newFile;
	                // and show it:
	                textView_filename.setText(newFile, TextView.BufferType.EDITABLE);
	            } else {//if(resultCode == this.RESULT_OK) {
	                Toast.makeText(
	                    this, 
	                    "Received NO result from file browser",
	                    Toast.LENGTH_LONG)
	                .show(); 
	            }//END } else {//if(resultCode == this.RESULT_OK) {
	        }//if (requestCode == REQUEST_CODE_PICK_FILE_TO_SAVE_INTERNAL) {
	        super.onActivityResult(requestCode, resultCode, data);
	}
	// ------------------------
	// file browser integration (end) 
	// ------------------------
	

}
