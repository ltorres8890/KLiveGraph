/*
 * Copyright (c) 2012 Luis A. Torres
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, 
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.kaiserdev.klivegraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class KLiveGraph extends View {

	/** Contains the Supported Graphing Styles by the graphing engine**/
	public enum GraphStyle{
		/**
		 * Draws the data as a continuous line.
		 */
		CONTINUOUS,
		/**
		 * Draw the data as an impulse train. (A series of lines from the x-axis to the value)
		 */
		IMPULSE,
		/**
		 * Draw the Data as a series of points in space.
		 */
		DOTTED
		}
		
	//Global View Parameters
	private float windowHeight;
	private float windowWidth;

	/****Graph Fields****/
	// Set of all data points.
	private ArrayList<Double> dataBuffer;
	
	//Paints for drawing all elements of the view.
	private Paint backgroundPaint;
	private Paint gridPaint;
	private Paint axisPaint;
	private Paint signalPaint;
	private Paint fontPaint;
	
	// contains the data points to be drawn on the screen
	private Path signalPath; 
	
	//Signal Graphing Parameters
	private int xScaler;
	private double yScaler;
	private double bias;
	private double maxValue;
	private double minValue;
	private GraphStyle gs;
	
	//Grid drawing parameters
	private int xGridSpace;
	private int yGridSpace;
	private double	xRefScale;
	private double yRefScale;
	private String xUnits;
	private String yUnits;
	
	//Scrolling-Related Graph Fields
    private int startIndex;
    private int endIndex;
    private boolean endSync; // graph the latest data points.
    private float currentX; //Historical point for determining amount of scrolling
    
    //Buffer for AutoScaling
    private List<Double> sortedBuffer;
    private boolean setAutoScale;
    private int sampleCount;
    private boolean isEnoughData;
    
    
    /*Constructors*/
	
    public KLiveGraph(Context context) {
		super(context);
        initView(context);
	}
	public KLiveGraph(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initView(context);
    }
    public KLiveGraph(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    
    /*View Initialization*/
    
	private void initView(Context context)
	{
		dataBuffer = new ArrayList<Double>();
		sortedBuffer = new ArrayList<Double>();
		
		signalPath = new Path();
		
		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		backgroundPaint.setAlpha(255);
		
		signalPaint = new Paint();
		signalPaint.setARGB(160,255,69,69);//(100, 12, 232, 93);
		signalPaint.setStrokeWidth((float)5.0);
		signalPaint.setStyle(Paint.Style.STROKE);
		
		gridPaint = new Paint();
		gridPaint.setARGB(60, 134, 222, 245);
		
		axisPaint = new Paint();
		axisPaint.setARGB(90, 8, 156, 168);
		axisPaint.setStrokeWidth((float)3.0);
		
		fontPaint = new Paint();
		fontPaint.setARGB(155, 155, 155, 150);
		fontPaint.setStrokeWidth((float)10.0);
		fontPaint.setTextSize((float) 18.0);
		
		initGraphWindow();
		resetScale();
	}
	
	private void initGraphWindow(){
		startIndex = 0;
		endIndex = dataBuffer.size();
		endSync = true;
		gs = GraphStyle.CONTINUOUS;
	}
	
	/**
	 * Returns all parameters to default settings.
	 * X-Scale = 1
	 * Y-Scale = 5
	 * Bias = 0.0
	 * Auto Scale Sampling rate = 50 Samples
	 */
	public void resetScale(){
		xScaler = 1;
		yScaler = 5.0;
		bias = 0.0;
		sampleCount = 50;
		this.invalidate();
	}
    
	
	/*Configuration (Setters)*/
	
	/**
	 * Enables Auto Scale functionality every 50 data points received.
	 * Equivalent to setAutoScale(boolean, 50);
	 * @param setAuto If true, Auto Scale functionality is enabled
	 */
	public void setAutoScale(boolean setAuto)
	{
		setAutoScale(setAuto,50);
	}
	
	/**
	 * Enables Auto Scale functionality to be called periodically.
	 * The Auto Scale function will be called according to the number determined.
	 * Setting count too low may cause the system to freeze if the data is received too quickly. 
	 * @param setAuto If true, Auto Scale functionality is enabled
	 * @param count The Amount of received data points between scaling calls. 
	 */
	public void setAutoScale(boolean setAuto, int count)
	{
		this.sampleCount = count;
		this.setAutoScale = setAuto;
	}
	
	/**
	 * Set the distance between data points on the x-Axis
	 * Setting any value below 1 will have no effect.
	 * @param xS The distance between points, in pixels (dP)
	 */
	public void setXScale(int xS)
	{
		if(xScaler < 1){return;}
		this.xScaler = xS;
	}
	
	/**
	 * Sets the factor to multiply the data points to fit the screen.
	 * It is recommended to use the method setMax and setMin instead.
	 * @param yS The Scaler value for the data in the y-Axis.
	 */
	public void setYScale(double yS)
	{
		this.yScaler = yS;
	}
	
	/**
	 * Set a corrective offset on the data before graphing.
	 * This offset will be applied before any scaling is performed, 
	 * it may affect the engine's capability to graph inside the screen's dimensions.
	 * @param bias
	 */
	public void setBias (double bias)
	{
		this.bias = bias;
	}
	
	/**
	 * Set the Minimum value expected to be graphed, for scaling purposes.
	 * @param minValue The lowest value expected.
	 */
	public void setMin(double minValue)
	{
		this.minValue = minValue;
		setScaler();
	}
	
	/**
	 * Set the Maximum value expected to be graphed, for scaling purposes.
	 * @param MaxValue The highest value expected.
	 */
	public void setMax(double maxValue)
	{
		this.maxValue = maxValue;
		setScaler();
	}
	
	/**
	 * Sets the 32-bit ARGB color for the data line on the graph.
	 * @param p
	 */
	public void setGraphPaint(int color)
	{
		this.signalPaint.setColor(color);
	}

	/**
	 * Select the style in which the view will graph the data.
	 * @param gS The graphing style as defined in KLiveGraph.GraphingStyle
	 */
	public void setGraphStyle(GraphStyle gS)
	{
		this.gs = gS;
	}

	/**
	 * Define the x-Axis for grid drawing.
	 * i.e. if 20 data points are received in 1 millisecond, 
	 * the way to call this function would be:
	 * setXGridSpace(20,1,"ms");
	 * @param space The amount of space, in data points, between marks.
	 * @param scale The scalar resolution of the grid.
	 * @param units The measurement units of the grid.
	 */
	public void setXGridSpace(int space, double scale, String units)
	{
		xGridSpace = space;
		xRefScale = scale;
		xUnits = units;
	}
	
	/**
	 * Define the Y-Axis for grid drawing
	 * This method is marginally different to its x-Axis counterpart.
	 * It will draw a line in multiples of the value defined in scale.
	 * Scale must be lower than the maximum value or no y-grid will be drawn.
	 * @param scale The value between horizontal lines on the grid
	 * @param units The measurement units.
	 */
	public void setYGridSpace(double scale, String units)
	{
		yRefScale = scale;
		yUnits = units;
	}

	
	/*Data Manipulation*/
		
	/**
	 * Adds a new data point to the end of the buffer and redraws the graph.
	 * @param point the newest value to be added
	 */
	public void receive(double point)
	{
		dataBuffer.add(point);
		isEnoughData = dataBuffer.size() % sampleCount == 0;
		if(isEnoughData && setAutoScale){autoScale();}
		this.invalidate();
	}
	
	/**
	 * Modifies the Y-Scale for optimum display of the data.
	 * Calling this function will change any values set using setMaxValue(double) and setMinValue(double)
	 */
	public void autoScale(){
		if(dataBuffer.size() == 0){return;}
		maxValue = Collections.max(dataBuffer);
		minValue = Collections.min(dataBuffer);
		setScaler();
		this.invalidate();
	}

	/**
	 * Removes all points from the buffer, effectively clearing the graph.
	 */
	public void clearGraph(){
		dataBuffer.clear();
		sortedBuffer.clear();
		this.invalidate();
	}
		
	/**
	 * Sets the optimum value for Y-scaler based on max and min values
	 */
	private void setScaler()
	{
		double value = Math.abs(maxValue) > Math.abs(minValue) ? Math.abs(maxValue) : Math.abs(minValue);
		yScaler = (windowHeight/value);
	}

	
	/*UI Events*/
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if(event.getAction() == MotionEvent.ACTION_DOWN)
    	{
    		currentX = event.getX();
    		return true;
    	}
    	else if(event.getActionMasked() == MotionEvent.ACTION_MOVE)
    	{
    		float deltaX = event.getX() - currentX;
    		currentX = event.getX();
    		moveGraphWindow(deltaX);
    		return true;
    	}
    	return super.onTouchEvent(event);
    }

    private void moveGraphWindow(float dX){
    	endIndex -= dX/xScaler;
    	endSync = false;
    	if(endIndex > dataBuffer.size() || dataBuffer.size() < windowWidth/xScaler) 
    	{
    		endIndex = dataBuffer.size();
    		endSync = true;
    	}
    	if(endIndex < 0){endIndex = 0;}
    	if(dataBuffer.size() > windowWidth/xScaler)// enough data to fill screen
    	{
    		if(endIndex < windowWidth/xScaler)
    		{
    			endIndex = (int)windowWidth/xScaler;
    		}
    	}
    	
    	this.invalidate();
    }    		
    
    
	/*View Drawing*/
    
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);		
		setIndexes();
		
		prepareGraphPath();
		
		canvas.drawPaint(backgroundPaint);
		
		drawGrid(canvas);
		
		canvas.drawPath(signalPath, signalPaint);
		
	}

	private void prepareGraphPath()
	{
		signalPath.reset();
		float startPos = dataBuffer.size() > 0 ? condition(dataBuffer.get(startIndex)):(windowHeight/2); 
		signalPath.moveTo(0, startPos);
		
		//will draw point while there is points to draw
		for(int i = startIndex+1, x = 0; i<endIndex; i++, x+= xScaler)
		{
			float xpt = (float)x;
			float ypt = condition(dataBuffer.get(i));
			
			if(gs == GraphStyle.CONTINUOUS)
			{ 
				signalPath.lineTo(xpt,ypt);
			}
			if(gs == GraphStyle.IMPULSE){
				signalPath.moveTo(xpt, 0);
				signalPath.lineTo(xpt,ypt);
				signalPath.addCircle(xpt, ypt, (float)3.0, Path.Direction.CW);
			}
			if(gs == GraphStyle.DOTTED){
				signalPath.addCircle(xpt, ypt, (float)3.0, Path.Direction.CW);
			}
		}
	}
	
	private void drawGrid(Canvas canvas)
	{
		//draw base line
		canvas.drawLine(0, windowHeight/2, windowWidth, windowHeight/2, axisPaint);
		
		//Draw X-Grid
		for(int i = 0, y = 0;i<windowWidth;i += xGridSpace , y++)
		{
			canvas.drawLine((float)i, (float)0.0, (float)i, (float)windowHeight, gridPaint);
			String current = (xRefScale*y) + xUnits;
			canvas.drawText(current, i+10, (float)windowHeight-10, fontPaint);
		}
		
		//Draw Y-Grid
		int YOffset = (int)condition(yRefScale);
		for(int i= (int)(windowHeight/2), j = (int)(windowHeight/2), k= 1, l = -1; (i < windowHeight || j>0); k++,l--)
		{
			i +=YOffset;
			j -=YOffset;
			//draw negative line
			canvas.drawLine(0, i, windowWidth, i, gridPaint);
			canvas.drawText((l*yRefScale)+yUnits, 0, i-5, fontPaint);
			
			//draw positive line
			canvas.drawLine(0, j, windowWidth, j, gridPaint);
			canvas.drawText((k*yRefScale)+yUnits, 0, i-5, fontPaint);
		}

	}
	
	/**
	 * Takes the value about to be added into the path and determines the equivalent Y-value
	 * @param d the point to Prepare
	 * @return The Y-value of the point to be added to the path.
	 */
	private float condition(Double d)
	{
		return (float) ((windowHeight/2) - ((d-bias)*yScaler));
	}
	
	/* Determines segment of the data set to be drawn */
	private void setIndexes(){
		//Set end Index
		endIndex = endSync ? dataBuffer.size():endIndex;
		
		//Set start Index
		startIndex = endIndex - (int)windowWidth/xScaler;
		if(startIndex < 0){
			startIndex = 0;
		}
	}
		
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		int parentH = MeasureSpec.getSize(heightMeasureSpec);
		int parentW = MeasureSpec.getSize(widthMeasureSpec);
		windowHeight = (float) parentH;
		windowWidth = (float) parentW;
		setMeasuredDimension(parentW,parentH);
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}



}

//TODO make sure everything works. JAJAJAJAJAJ GOOD ONE!
//TODO Refactor testing

