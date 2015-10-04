package rz.remotesteer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class VerticalSeekBar extends SeekBar {

  public VerticalSeekBar(Context context) {
        super(context);
    }


  public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


  public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(h, w, oldh, oldw);
  }


  @Override
  protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(heightMeasureSpec, widthMeasureSpec);
    setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
  }


  protected void onDraw(Canvas c) {
    c.rotate(-90);
    c.translate(-getHeight(), 0);
    super.onDraw(c);
  }


  @Override public synchronized void setProgress(int progress) {
    super.setProgress(progress);
    onSizeChanged(getWidth(), getHeight(), 0, 0);
  }


  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch_position_changed_ = false;
    if (!isEnabled()) {
      return false;
    }
    switch (event.getAction()) {
      case MotionEvent.ACTION_UP:
        super.onTouchEvent(event);
        switch_position_changed_ = false;
        break;
      case MotionEvent.ACTION_DOWN:
        switch_position_changed_ = true;
      case MotionEvent.ACTION_MOVE:
        // Here is the equation for computing regular seek progress:
        // setProgress(getMax() - (int)(getMax() * (event.getY() / getHeight())));
        float max = getMax();
        float half = max / 2;
        float current_value = max * (event.getY() / getHeight());
        if (current_value < half * 2 / 3) {
          setProgress((int)max);
        } else if (current_value > half * 4 / 3) {
          setProgress(0);
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        break;
    }
    return true;
  }

  public boolean getSwitchPositionChanged() {
    return switch_position_changed_;
  }

  public void setSwitchPositionChanged(boolean switch_position_changed) {
    switch_position_changed_ = switch_position_changed;
  }

  // Indicates whether the seeker, acting as a switch, changed its position.
  private boolean switch_position_changed_;
}

