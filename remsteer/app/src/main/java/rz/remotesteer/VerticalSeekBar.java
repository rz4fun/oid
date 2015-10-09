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


  public void SetControlMode(int control_mode) {
    control_mode_ = control_mode;
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
    if (control_mode_ == CONTROL_MODE_SLIDE) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
          super.onTouchEvent(event);
          break;
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_MOVE:
          setProgress(getMax() - (int)(getMax() * (event.getY() / getHeight())));
          break;
        case MotionEvent.ACTION_CANCEL:
          break;
      }
    } else if (control_mode_ == CONTROL_MODE_SWITCH) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
          super.onTouchEvent(event);
          switch_position_changed_ = false;
          break;
        case MotionEvent.ACTION_DOWN:
          switch_position_changed_ = true;
        case MotionEvent.ACTION_MOVE:
          float max = getMax();
          float half = max / 2;
          float current_value = max * (event.getY() / getHeight());
          if (current_value < half * 2 / 3) {
            setProgress((int) max);
          } else if (current_value > half * 4 / 3) {
            setProgress(0);
          }
          break;
        case MotionEvent.ACTION_CANCEL:
          break;
      }
    }
    return true;
  }

  public boolean getSwitchPositionChanged() {
    return switch_position_changed_;
  }

  public void setSwitchPositionChanged(boolean switch_position_changed) {
    switch_position_changed_ = switch_position_changed;
  }

  // In the SWITCH control mode, this flag indicates that the seeker had changed its position.
  private boolean switch_position_changed_;

  private int control_mode_ = CONTROL_MODE_SLIDE;

  public static final int CONTROL_MODE_SLIDE = 1;
  public static final int CONTROL_MODE_SWITCH = 2;

}

