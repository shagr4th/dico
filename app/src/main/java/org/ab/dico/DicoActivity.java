package org.ab.dico;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DicoActivity extends Activity /* implements SearchDict.ISearch*/ {
    //private SearchDict dict;
	
	private SQLiteDatabase db;
	final Handler mHandler = new Handler();
	private StringBuffer sb;
	private ProgressDialog dialog;
	
	
	// Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
        	((TextView)findViewById(R.id.contents)).setText(sb.toString());
        	if (dialog != null && dialog.isShowing()) {
        		dialog.dismiss();
        		dialog = null;
        	}
        }
    };
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        String db_path = getApplicationInfo().dataDir + "/databases/";
        String db_name = "dico.db";
        new File(db_path).mkdirs();
        File dbFile = new File(db_path, db_name);
        
        try {
        	if (!dbFile.exists()) {
        		Log.i("dico", "Copying database...");
    	        InputStream mInput = getAssets().open(db_name);
    	        OutputStream mOutput = new FileOutputStream(dbFile);
    	        byte[] mBuffer = new byte[8192];
    	        int mLength;
    	        while ((mLength = mInput.read(mBuffer))>0)
    	        {
    	            mOutput.write(mBuffer, 0, mLength);
    	        }
    	        mOutput.flush();
    	        mOutput.close();
    	        mInput.close();
            }
            
            Log.i("dico", "Loading database...");
        	db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY + SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        	Log.i("dico", "Database ok");
        } catch (Exception sqe) {
        	sqe.printStackTrace();
        	Toast.makeText(this, "Error in opening dico.db : " + sqe.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        findViewById(R.id.search).setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					if (event.getAction() == KeyEvent.ACTION_UP) {
						launchSearch();
					}
					return true;
				}
				return false;
			}
		});
        
        findViewById(R.id.button_clear).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	((EditText)findViewById(R.id.search)).setText("");
		    	((TextView)findViewById(R.id.contents)).setText("");
		    }
        });
        
        findViewById(R.id.button_search).setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	launchSearch();
		    }
		});
    }
    
    private void launchSearch() {
    	final String word = ((EditText)findViewById(R.id.search)).getText().toString();
    	if (db != null && word != null) {
    		((EditText)findViewById(R.id.search)).selectAll();
        	Thread t = new Thread() {
                public void run() {
		    		Cursor c = db.rawQuery("SELECT word from DICT where word LIKE ?", new String [] { word.toUpperCase().trim() });
		    		sb = new StringBuffer();
		    		int ci = c.getColumnIndex("word");
		    		if (c.moveToFirst()) {
		    			int i = 0;
		    			sb.append("Correspondances: (");
		    			sb.append(c.getCount());
		    			sb.append(")");
		    			sb.append("\n");
		    			do {
		    				sb.append(c.getString(ci));
		    				sb.append("\n");
		    				i++;
		    			} while (c.moveToNext() && i < 1000);
		    			((TextView)findViewById(R.id.contents)).scrollTo(0, 0);
		    		}
		    		c.close();
		    		if (sb.length() == 0)
		    			sb.append("Pas de correspondances!");
		    		mHandler.post(mUpdateResults);
                }
    		};
    		dialog = ProgressDialog.show(DicoActivity.this, "", "Searching. Please wait...", true);
    		t.start();
    	} else {
    		((TextView)findViewById(R.id.contents)).setText(db == null?"dico.db non chargé !":"mot invalide ?!");
    	}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i("dico", "Closing database...");
		if (db != null)
			db.close();
	}
    
    
/*
	@Override
	public InputStream getDictInputStream() {
		return getResources().openRawResource(R.raw.ods5);
	}

	@Override
	public void setInfo(String info) {
		setTitle(info);
	}

	@Override
	public void setList(Vector<String> words) {
		int max = 100;
		if (words.size() < max)
			max = words.size();
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<max;i++) {
			sb.append(words.get(i));
			sb.append("\n");
		}
		((TextView)findViewById(R.id.contents)).setText(sb.toString());
	}*/
}