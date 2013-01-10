package cn.whzxt.android;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.SystemClock;

public class StreamIt implements Camera.PreviewCallback {
	public Boolean Screenshot = false;
	public BitmapFactory.Options options = null;
	private ByteArrayOutputStream output_stream = null;
	public byte[] yuv420sp = null;
	private byte[] tempBytes = null;
	private YuvImage yuv_image = null;
	private Rect rect = null;
	private int w = 0;
	private int h = 0;
	private int format = 0;
	private Camera.Parameters parameters = null;
	private Boolean vb = false;

	public StreamIt() {
		options = new BitmapFactory.Options();
		options.inSampleSize = 4;
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		parameters = camera.getParameters();
		format = parameters.getPreviewFormat();

		// YUV formats require more conversion
		if (format == ImageFormat.NV21) {
			if (Screenshot || yuv420sp == null || (Train.IsTraining && !Train.VideoPath.equals(""))) {
				w = parameters.getPreviewSize().width;
				h = parameters.getPreviewSize().height;
				// Get the YuV image
				yuv_image = new YuvImage(data, format, w, h, null);
				// Convert YuV to Jpeg
				rect = new Rect(0, 0, w, h);
				output_stream = new ByteArrayOutputStream();
				yuv_image.compressToJpeg(rect, 100, output_stream);
				tempBytes = output_stream.toByteArray();
				output_stream = new ByteArrayOutputStream();
				BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.length, options).compress(CompressFormat.JPEG, 80, output_stream);
				yuv420sp = output_stream.toByteArray();

				if (Train.IsTraining && !Train.VideoPath.equals("")) {
					vb = !vb;
					if (vb) {
						// 保存照片
						File file = new File(Train.VideoPath + "/" + String.format("%05d.jpg", ++Train.VideoPhotoCount));
						try {
							file.createNewFile();
							BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
							os.write(yuv420sp);
							// BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.length, options).compress(Bitmap.CompressFormat.JPEG, 50, os);
							os.flush();
							os.close();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			SystemClock.sleep(100);
		}
	}
}