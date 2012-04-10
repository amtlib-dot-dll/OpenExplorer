package org.brandroid.openmanager.fragments;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.net.ftp.FTPFile;
import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.SettingsActivity;
import org.brandroid.openmanager.adapters.ArrayPagerAdapter;
import org.brandroid.openmanager.data.OpenFTP;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenNetworkPath;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.data.OpenServer;
import org.brandroid.openmanager.data.OpenServers;
import org.brandroid.openmanager.ftp.FTPManager;
import org.brandroid.utils.Logger;

import android.content.Context;
import android.inputmethodservice.InputMethodService.InputMethodImpl;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

public class TextEditorFragment extends OpenFragment implements OnClickListener
{
	private EditText mEditText;
	private TextView mPathLabel;
	private ProgressBar mProgress;
	private Button mSave, mCancel;
	
	private OpenPath mPath = null;
	private String mData = null;
	
	private AsyncTask mTask = null;
	
	private boolean bShowKeyboard = true;
	
	public TextEditorFragment() { }
	public TextEditorFragment(OpenPath path)
	{
		mPath = path;
		Bundle b = new Bundle();
		if(path != null && path.getPath() != null)
			b.putString("edit_path", path.getPath().toString());
		setArguments(b);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = savedInstanceState;
		if(savedInstanceState == null && getArguments() != null)
			bundle = getArguments();
		if(bundle != null && bundle.containsKey("edit_path"))
		{
			String path = bundle.getString("edit_path");
			mPath = new OpenFile(path);
			Logger.LogDebug("load text editor (" + path + ")");
			if(bundle.containsKey("edit_data"))
				mData = bundle.getString("edit_data");
			if(bundle.containsKey("edit_server"))
			{
				int serverIndex = bundle.getInt("edit_server");
				Logger.LogDebug("Loading server #" + serverIndex);
				if(serverIndex > -1)
				{
					OpenServers servers = SettingsActivity.LoadDefaultServers(getActivity());
					if(serverIndex < servers.size())
					{
						OpenServer server = servers.get(serverIndex);
						FTPManager man = new FTPManager(server.getHost(), server.getUser(), server.getPassword(), server.getPath()); 
						Logger.LogDebug("Found server - " + server.getName());
						mPath = new OpenFTP(mPath.getPath(), null, man);
					}
				}
			} else if (path.indexOf("ftp:/") > -1)
			{
				path = path.substring(path.lastIndexOf("/", path.indexOf(":/") + 2) + 1);
				String host = path;
				if(path.indexOf("/") > -1)
				{
					host = path.substring(0, path.indexOf("/"));
					path = path.substring(path.indexOf("/"));
				}
				OpenServers servers = SettingsActivity.LoadDefaultServers(getActivity());
				for(int i=0; i < servers.size(); i++)
					if(servers.get(i).getHost().equals(host))
					{
						OpenServer server = servers.get(i);
						FTPManager man = new FTPManager(server.getHost(), server.getUser(), server.getPassword(), server.getPath());
						Logger.LogDebug("Searched & Found server - " + server.getName());
						mPath = new OpenFTP(mPath.getPath(), null, man);
					}
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.edit_text, null);
		mEditText = (EditText)view.findViewById(R.id.text_edit);
		mPathLabel = ((TextView)view.findViewById(R.id.label_path));
		mProgress = ((ProgressBar)view.findViewById(R.id.progress));
		mSave = ((Button)view.findViewById(R.id.btn_save));
		mCancel = ((Button)view.findViewById(R.id.btn_cancel));
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mSave.setOnClickListener(this);
		mCancel.setOnClickListener(this);
		((Button)view.findViewById(R.id.btn_toggle_keyboard)).setOnClickListener(this);
		mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				/*if(hasFocus)
				{
					InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(v, 0);
				}*/
			}
		});
		if(mPath != null && mData == null)
			mTask = new FileLoadTask().execute(mPath);
		else if (mData != null)
			mEditText.setText(mData);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mData = mEditText.getText().toString();
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mData = mEditText.getText().toString();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.LogInfo("saveInstanceState @ TextEditor (" + mPath.getPath() + ")");
		outState.putString("edit_path", mPath.getPath());
		if(mData != null && mData.length() < 500000)
			outState.putString("edit_data", mData);
		if(mPath instanceof OpenFTP && ((OpenFTP)mPath).getServersIndex() > -1)
		{
			Logger.LogDebug("Saving server #" + ((OpenFTP)mPath).getServersIndex());
			outState.putInt("edit_server", ((OpenFTP)mPath).getServersIndex());
		} else Logger.LogDebug("No server #");
	}
	
	public void save()
	{
		cancelTask();
		mTask = new FileSaveTask(mPath);
		((FileSaveTask)mTask).execute(mEditText.getText().toString());
	}
	
	public void cancelTask() {
		if(mTask != null)
			mTask.cancel(true);
	}

	public void onClick(View v) {
		switch(v.getId())
		{
			case R.id.btn_save:
				save();
				break;
			case R.id.btn_cancel:
				cancelTask();
				if(getExplorer().isViewPagerEnabled())
				{
					ViewPager pager = (ViewPager)getExplorer().findViewById(R.id.content_pager);
					ArrayPagerAdapter adapter = (ArrayPagerAdapter)pager.getAdapter();
					if(pager.getCurrentItem() > 0)
						pager.setCurrentItem(pager.getCurrentItem() - 1);
					adapter.remove(this);
					pager.invalidate();
				} else
					getFragmentManager().popBackStack();
				break;
			case R.id.btn_toggle_keyboard:
				//if(((InputMethodManager)getActivity()).getInputMethodList().
				((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(mEditText, 0);
				break;
		}
	}

	
	public static void setEnabled(boolean enabled, View... views)
	{
		for(View v : views)
			if(v != null)
				v.setEnabled(enabled);
	}
	
	
	public class FileSaveTask extends AsyncTask<String, Integer, String>
	{
		private OpenPath mPath;
		
		public FileSaveTask(OpenPath path)
		{
			mPath = path;
		}

		@Override
		protected String doInBackground(String... datas) {
			String data = datas[0];
			OutputStream fos = null;
			try {
				fos = new BufferedOutputStream(mPath.getOutputStream());
				fos.write(data.getBytes());
				fos.close();
				if(OpenFTP.class.equals(mPath.getClass()))
					((OpenFTP)mPath).getManager().disconnect();
			} catch(Exception e) {
				Logger.LogError("Couldn't save file.", e);
			}
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if(mProgress != null)
				mProgress.setVisibility(View.VISIBLE);
			setEnabled(false, mEditText, mSave);
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(mProgress != null)
				mProgress.setVisibility(View.GONE);
			setEnabled(true, mEditText, mSave);
		}
		
	}
	
	public class FileLoadTask extends AsyncTask<OpenPath, Integer, String>
	{
		@Override
		protected String doInBackground(OpenPath... params) {
			OpenPath mPath = params[0];
			String path = mPath.getPath();
			Logger.LogDebug("Getting " + path);
			if(mPath.canRead()) {
				Logger.LogDebug("File is " + mPath.length() + " bytes.");
				InputStream is = null;
				StringBuilder sb = new StringBuilder();
				try {
					is = mPath.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String line;
					while((line = br.readLine()) != null)
						sb.append(line + "\n");
				} catch (RuntimeException r) {
					Logger.LogError("File too large?", r);
					getExplorer().showToast("Unable to open file. File too large?");
					cancelTask();
					getFragmentManager().popBackStack();
				} catch (Exception e) {
					Logger.LogError("Couldn't find file - " + path, e);
				} finally {
					try {
						if(is != null)
							is.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(OpenFTP.class.equals(mPath.getClass()))
						((OpenFTP)mPath).getManager().disconnect();
					else if(mPath instanceof OpenNetworkPath)
						((OpenNetworkPath)mPath).disconnect();
				}
				return sb.toString();
			}
			else if(path.indexOf("ftp:/") > -1)
			{
				try {
					URL url = new URL(path);
					FTPManager ftp = FTPManager.getInstance(url);
					BufferedInputStream in = (BufferedInputStream)ftp.getInputStream(url.getPath());
					byte[] buffer = new byte[4096];
					StringBuilder sb = new StringBuilder();
					while(in.read(buffer) > 0)
					{
						for(byte b : buffer)
							sb.append((char)b);
					}
					return sb.toString();
				} catch (MalformedURLException e) {
					Logger.LogError("Bad URL for FTP - " + path, e);
				} catch (IOException e) {
					Logger.LogError("Couldn't read from FTP - " + path, e);
				}
				
				//return FTPManager.getData(path);
			} else if(path.indexOf("sftp:/") > -1)
			{
				try {
					BufferedInputStream in = (BufferedInputStream)mPath.getInputStream();
					byte[] buffer = new byte[4096];
					StringBuilder sb = new StringBuilder();
					while(in.read(buffer) > 0)
					{
						for(byte b : buffer)
							sb.append((char)b);
					}
					return sb.toString();
				} catch(IOException e) {
					Logger.LogError("Couldn't read from SFTP - " + path, e);
				}
			}
			return null;
		}
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			setEnabled(false, mEditText, mSave, mCancel);
			if(mProgress != null)
				mProgress.setVisibility(View.VISIBLE);
			if(mPathLabel != null)
				mPathLabel.setText("Loading " + mPath.getPath());
		}
		
		@Override
		protected void onPostExecute(String result) {
			if(mEditText != null)
				mEditText.setText(result);
			if(mProgress != null)
				mProgress.setVisibility(View.GONE);
			if(mPathLabel != null)
				mPathLabel.setText(mPath.getName());
			setEnabled(true, mEditText, mSave, mCancel);
			mData = result;
		}
	}

	public OpenPath getPath() {
		// TODO Auto-generated method stub
		return mPath;
	}
	@Override
	public CharSequence getTitle() {
		// TODO Auto-generated method stub
		return getPath().getName();
	}
}
