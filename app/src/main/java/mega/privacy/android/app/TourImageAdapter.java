package mega.privacy.android.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class TourImageAdapter extends PagerAdapter {

	private int[] mImages = new int[] {
	        R.drawable.storage_space,
	        R.drawable.speed,
	        R.drawable.privacy_security,
	        R.drawable.access
	    };	
	
	private Activity activity;
	private LayoutInflater inflater;
	
	// constructor
	public TourImageAdapter(Activity activity) {
		this.activity = activity;
	}
	
	@Override
	public int getCount() {
		return mImages.length;
	}

	@Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }
		
	@Override
    public Object instantiateItem(ViewGroup container, int position) {
        ImageView imgDisplay;
 
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewLayout = inflater.inflate(R.layout.tour_image_layout, container,false);
 
        imgDisplay = (ImageView) viewLayout.findViewById(R.id.imageTour);

        imgDisplay.setImageResource(mImages[position]);

		/*Rounded corners of image*/

		Bitmap bitmap = ((BitmapDrawable)imgDisplay.getDrawable()).getBitmap();
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		int radius =20;
		Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final RectF rectF = new RectF(0, 0, w, h);

		canvas.drawRoundRect(rectF, radius, radius, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, null, rectF, paint);

		/*left top and right top corners*/

		final Rect clipRect = new Rect(0, radius, w, h);
		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
		canvas.drawRect(clipRect, paint);

		paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, null, rectF, paint);

		imgDisplay.setImageBitmap(output);

        ((ViewPager) container).addView(viewLayout);
 
        return viewLayout;
	}
	
	@Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ((ViewPager) container).removeView((LinearLayout) object); 
    }

}
