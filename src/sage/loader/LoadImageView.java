package sage.loader;


import java.io.File;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

public class LoadImageView{
	public static final int taskTagID = 1357924680;
	
	public static interface OnImageLoadingListener{
		public Bitmap onImageLoading(String path);
	}//interface
	
	public static interface OnImageLoadedListener{
		public void onImageLoaded(boolean isSuccess,Bitmap bmp,View view);
	}//interface
	
	
    /*========================================================
	*/	
	//Starting Point, Load the image through a thread
	public static void loadImage(String imgPath,View view,Object context){
		if(cancelRunningTask(imgPath,view)){
			final LoadingTask task = new LoadingTask(view,context);
			view.setTag(taskTagID,task);
			task.execute(imgPath);
		}//if
	}//func
	
	//if a task is already running, cancel it.
	public static boolean cancelRunningTask(String imgPath,View view){
		final LoadingTask task = getLoadingTask(view);
		
		if(task != null){
			final String taskImgPath = task.imagePath;
			
			if(taskImgPath == null || !taskImgPath.equals(imgPath)) task.cancel(true);
			else return false;
		}//if
		
		return true;
	}//func
		
	//Get the loading task from the imageview
	public static LoadingTask getLoadingTask(View view){
		if(view != null){
			final Object task = view.getTag(taskTagID);
			if(task != null && task instanceof LoadingTask){
				return (LoadingTask)task;
			}//if
		}//if
		
		return null;
	}//func


	//************************************************************
	// Load Image through thread
	//************************************************************
	protected static class LoadingTask extends AsyncTask{
		private WeakReference mImgView = null;
		private WeakReference mOnImageLoading = null;
		private WeakReference mOnImageLoaded = null;
		
		public String imagePath; //used to compare if the same task
		
		public LoadingTask(View view,Object context){
			mImgView = new WeakReference(view);

			if(context != null){
				if(context instanceof OnImageLoadingListener) mOnImageLoading = new WeakReference((OnImageLoadingListener)context);
				if(context instanceof OnImageLoadedListener) mOnImageLoaded = new WeakReference((OnImageLoadedListener)context);
			}//if
		}//func

		@Override
		protected Bitmap doInBackground(Object... params){
			imagePath = (String)params[0];
			
			//.....................................			
			//If callback exists, then we need to load the image in a special sort of way.
			if(mOnImageLoading != null){
				final OnImageLoadingListener callback = (OnImageLoadingListener)mOnImageLoading.get();
				return (callback != null)? callback.onImageLoading(imagePath) : null;
			}//if

			//.....................................
			//if no callback, then load the image ourselves
			File fImg = new File(imagePath);
			if(!fImg.exists()) return null;
				
			return BitmapFactory.decodeFile(imagePath,null);
		}//func

		@Override
		protected void onPostExecute(Object bmp){
			boolean isSuccess = false;
			
			//--------------------------
			//if the task has been cancelled, don't bother doing anything else.
			if(this.isCancelled()){
				if(bmp != null){ ((Bitmap)bmp).recycle(); bmp = null; }

			//--------------------------
			//if no callback, but we have an image and a view.
			}else if(mImgView != null && bmp != null){
				final View view = (View) mImgView.get();
				if(view != null && view instanceof ImageView && bmp != null){
					((ImageView)view).setImageBitmap((Bitmap)bmp);
					isSuccess = true;
				}//if
			
			//--------------------------
			//incase imageview doesn't exist anymore but bmp was loaded.
			}else if(bmp != null){ ((Bitmap)bmp).recycle(); bmp=null; }//if

			//.....................................
			//When done loading the image, alert parent
			if(mOnImageLoaded != null){
				final OnImageLoadedListener callback = (OnImageLoadedListener) mOnImageLoaded.get();
				if(callback != null) callback.onImageLoaded(isSuccess,(Bitmap)bmp,(View) mImgView.get());
			}//if
		}//func
	}//cls
}//func