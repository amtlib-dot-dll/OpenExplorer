package org.brandroid.openmanager.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.brandroid.openmanager.R;
import org.brandroid.openmanager.activities.OpenExplorer;
import org.brandroid.openmanager.data.OpenFile;
import org.brandroid.openmanager.data.OpenPath;
import org.brandroid.openmanager.util.ThumbnailCreator;
import org.brandroid.openmanager.views.RemoteImageView;

import android.text.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class OpenClipboard
	extends BaseAdapter
	implements List<OpenPath>
{
	private static final long serialVersionUID = 8847538312028343319L;
	public boolean DeleteSource = false;
	public boolean ClearAfter = true;
	private final ArrayList<OpenPath> list;
	private final Context mContext;
	private OnClipboardUpdateListener listener;
	private boolean mMultiselect = false;
	
	public interface OnClipboardUpdateListener
	{
		public void onClipboardUpdate();
		public void onClipboardClear();
	}
	
	@SuppressWarnings("deprecation")
	private void onClipboardUpdate()
	{
		super.notifyDataSetChanged();
		if(listener != null)
		{
			if(list.size() == 0)
				listener.onClipboardClear();
			else
				listener.onClipboardUpdate();
		}
		ClipboardManager clip = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
		StringBuilder clipText = new StringBuilder();
		for(int i = 0; i < size(); i++)
			clipText.append(get(i).getAbsolutePath() + "\n");
		clip.setText(clipText);
		if(!OpenExplorer.BEFORE_HONEYCOMB)
		{
			ClipData data = ClipData.newPlainText("files", clipText);
			((android.content.ClipboardManager)clip).setPrimaryClip(data);
		}
	}
	
	public OpenClipboard(Context context)
	{
		super();
		list = new ArrayList<OpenPath>();
		this.mContext = context;
		readSystemClipboard();
	}
	
	private void readSystemClipboard()
	{
		ClipboardManager sysboard = (ClipboardManager)mContext.getSystemService(Context.CLIPBOARD_SERVICE);
		if(!OpenExplorer.BEFORE_HONEYCOMB)
		{
			ClipData data = ((android.content.ClipboardManager)sysboard).getPrimaryClip();
			if(data != null)
			{
				for(int i = 0; i < data.getItemCount(); i++)
				{
					String txt = data.getItemAt(i).coerceToText(mContext).toString();
					for(String s : txt.split("\n"))
						if(s != null && new File(s).exists())
							add(new OpenFile(s));
				}
				return;
			}
		} else {
			@SuppressWarnings("deprecation")
			CharSequence clipText = sysboard.getText();
			if(clipText != null)
			{
				for(String s : clipText.toString().split("\n"))
					if(s != null && new File(s).exists())
						add(new OpenFile(s));
			}
		}
	}
	
	public void startMultiselect() {
		mMultiselect = true; 
		onClipboardUpdate();
	}
	public void stopMultiselect() {
		mMultiselect = false;
		onClipboardUpdate();
	}
	public boolean isMultiselect() { return mMultiselect; }
	
	public void setClipboardUpdateListener(OnClipboardUpdateListener listener)
	{
		this.listener = listener;
	}

	public int getCount() {
		return list.size();
	}

	public OpenPath getItem(int pos) {
		return list.get(pos);
	}
	
	public List<OpenPath> getAll() {
		return list;
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		View ret = convertView;
		if(ret == null)
		{
			ret = ((LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.list_content_layout, parent, false); 
		}
		int w = mContext.getResources().getDimensionPixelSize(R.dimen.list_icon_size);
		//ret.setLayoutParams(new Gallery.LayoutParams(w, w));
		//double sz = (double)w * 0.7;
		
		final OpenPath file = (OpenPath)getItem(position);
		
		TextView text = (TextView)ret.findViewById(R.id.content_text);
		if(text != null)
		{
			RemoteImageView image = (RemoteImageView)ret.findViewById(R.id.content_icon);
			TextView pathView = (TextView)ret.findViewById(R.id.content_fullpath);
			TextView info = (TextView)ret.findViewById(R.id.content_info);
			ImageView check = (ImageView)ret.findViewById(R.id.content_check);
			check.setVisibility(View.VISIBLE);
			
			info.setVisibility(View.GONE);
			text.setText(file.getName());
			pathView.setText(file.getPath());
			ThumbnailCreator.setThumbnail(image, file, w, w); //(int)(w * (3f/4f)), (int)(w * (3f/4f)));
			
		} else if(ret instanceof TextView) {
			text = (TextView)ret;
			text.setText(file.getName());
			text.setCompoundDrawables(
				ThumbnailCreator.getDefaultDrawable(file, 32, 32, parent.getContext()),
				(Drawable)null,
				parent.getContext().getResources().getDrawable(android.R.drawable.checkbox_on_background),
				(Drawable)null);
		}

		return ret;
	}

	public Iterator<OpenPath> iterator() {
		return list.iterator();
	}

	public View addPath(OpenPath path)
	{
		add(path);
		int pos = indexOf(path);
		return getView(pos, null, null);
	}
	public boolean add(OpenPath path) {
		boolean ret = true;
		if(list.contains(path))
			ret = false;
		ret = list.add(path);
		onClipboardUpdate();
		return ret;
	}

	public void add(int index, OpenPath path) {
		list.add(index, path);
		onClipboardUpdate();
	}

	public boolean addAll(Collection<? extends OpenPath> collection) {
		boolean ret = list.addAll(collection);
		onClipboardUpdate();
		return ret;
	}

	public boolean addAll(int index, Collection<? extends OpenPath> collection) {
		return list.addAll(index, collection);
	}

	public void clear() {
		list.clear();
		stopMultiselect();
		onClipboardUpdate();
	}

	public boolean contains(Object path) {
		return list.contains(path);
	}

	public boolean containsAll(Collection<?> paths) {
		return list.containsAll(paths);
	}

	public OpenPath get(int location) {
		return list.get(location);
	}

	public int indexOf(Object path) {
		return list.indexOf(path);
	}

	public int lastIndexOf(Object path) {
		return list.lastIndexOf(path);
	}

	public ListIterator<OpenPath> listIterator() {
		return list.listIterator();
	}

	public ListIterator<OpenPath> listIterator(int location) {
		return list.listIterator(location);
	}

	public OpenPath remove(int location) {
		OpenPath ret = list.remove(location);
		onClipboardUpdate();
		return ret;
	}

	public boolean remove(Object path) {
		boolean ret = list.remove(path);
		onClipboardUpdate();
		return ret;
	}

	public boolean removeAll(Collection<?> paths) {
		boolean ret = list.removeAll(paths);
		onClipboardUpdate();
		return ret;
	}

	public boolean retainAll(Collection<?> paths) {
		return list.retainAll(paths);
	}

	public OpenPath set(int index, OpenPath path) {
		OpenPath ret = list.set(index, path);
		onClipboardUpdate();
		return ret;
	}

	public int size() {
		return list.size();
	}

	public List<OpenPath> subList(int start, int end) {
		return list.subList(start, end);
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] array) {
		return list.toArray(array);
	}

}