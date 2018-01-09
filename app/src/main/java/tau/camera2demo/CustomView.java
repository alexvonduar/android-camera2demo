package tau.camera2demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by alex on 18-1-3.
 */

public class CustomView extends SurfaceView {
    private final Paint paint;
    private final SurfaceHolder mHolder;
    private final Context context;

    public CustomView(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        this.context = context;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            /*
            invalidate();
            if (mHolder.getSurface().isValid()) {
                final Canvas canvas = mHolder.lockCanvas();
                Log.d("touch", "touchRecieved by camera");
                if (canvas != null) {
                    Log.d("touch", "touchRecieved CANVAS STILL Not Null");
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    canvas.drawColor(Color.TRANSPARENT);
                    canvas.drawCircle(event.getX(), event.getY(), 100, paint);
                    mHolder.unlockCanvasAndPost(canvas);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Canvas canvas1 = mHolder.lockCanvas();
                            if(canvas1 !=null){
                                canvas1.drawColor(0, PorterDuff.Mode.CLEAR);
                                mHolder.unlockCanvasAndPost(canvas1);
                            }

                        }
                    }, 1000);

                }
                mHolder.unlockCanvasAndPost(canvas);


            }
            */
        }


        return false;
    }
}
