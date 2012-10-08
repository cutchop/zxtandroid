package cn.whzxt.android;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

public class PhotoViewDialog extends Dialog {

	Context context;
	private ImageView photoView;
	public PhotoViewDialog(Context context) {
		super(context);
		this.context = context;
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.photoview);
        this.setCanceledOnTouchOutside(true);
        photoView = (ImageView)findViewById(R.id.photoView);
    }
    
    public void setImage(Bitmap bmp){
    	photoView.setImageBitmap(bmp);
    }

}
