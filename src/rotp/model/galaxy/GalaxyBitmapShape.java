/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	 https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.galaxy;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import rotp.model.game.IGameOptions;
import rotp.ui.UserPreferences;

public class GalaxyBitmapShape extends GalaxyShape {
	public static final List<String> options1;
	public static final List<String> options2;
	private static final long serialVersionUID = 1L;
	private static final int GX = 127;
	private static final int GY = 127;
	private static final float GS = 127f/6f;
	private static enum Option1Enum {
		SETUP_BITMAP_GREY_SUM,
		SETUP_BITMAP_GREY_MAX,
		SETUP_BITMAP_GREY_INVERSE,
		SETUP_BITMAP_COLOR,
		SETUP_BITMAP_ADVANCED,
		SETUP_BITMAP_ADVANCED_MASK,
		SETUP_BITMAP_ADVANCED_MASK2
	}
	private static enum Option2Enum {
		SETUP_BITMAP_NORMAL,
		SETUP_BITMAP_SHARP1,
		SETUP_BITMAP_SHARP2,
		SETUP_BITMAP_SHARP3,
		SETUP_BITMAP_SHARP4,
	}
	static {
		options1 = new ArrayList<>();
		for (Option1Enum o : Option1Enum.values())
			options1.add(o.toString());
		options2 = new ArrayList<>();
		for (Option2Enum o : Option2Enum.values())
			options2.add(o.toString());
	}
	private float adjustDensity;
	private float aspectRatio;
	private float densityFactor;
	private float[][] starsPD; // Star Map
	private float[][] sharpPD; // for grey maps
	private float[][] redPD;   // Opponent Map
	private float[][] greenPD; // User Map
	private float[][] bluePD;  // Orion Map
	private float[][] starsCD;
	private float[][] userCD;
	private float[][] orionCD;
	private float[][] nebulaeCD;
	private float[][][] alienCD;

	private int xBM, yBM;
	private int alienSize;
	private int sharpNb, maskNb;
	private float offset, xMult, yMult, volume;
	private boolean isSum, isInverted, isColor, isAdvanced, isMask;
	private boolean isSharp;

	public GalaxyBitmapShape(IGameOptions options) {
		opts = options;
	}
	private float sqr(float x) { return x*x;}
	private void genGaussian(int sx, int sy, float sigma) {
		// Bad file, Generate a working grey Map
		xBM = sx;
		yBM = sy;
		isColor	= false;
		isAdvanced = false;
		starsPD	= new float[yBM][xBM];
		if (isSharp)
			sharpPD = new float[yBM][xBM];
		float cx = (-1.0f + xBM)/2.0f;
		float cy = (-1.0f + yBM)/2.0f;
		float s2 = -2*sigma*sigma;

		for (int y=0; y<yBM; y++) {
			float dy2 = sqr(y-cy);
			for (int x=0; x<xBM; x++) {
				float r2 = dy2 + sqr(x-cx);
				float level = (float) Math.exp(r2/s2);
				starsPD[y][x] = level;
				if (isSharp)
					sharpPD[y][x] = level;
			}
		}
	}
	private void sharpenPD(float[][] pD) {
		for (int y=0; y<yBM; y++)
			for (int x=0; x<xBM; x++)
				pD[y][x] *= pD[y][x];
	}
	private void invertPD(float[][] pD) {
		for (int y=0; y<yBM; y++)
			for (int x=0; x<xBM; x++)
				pD[y][x] = 1-pD[y][x];
		normalizeToOne(pD);
	}
	private void normalizeToOne(float[][] pD) {
		float max = Float.MIN_VALUE; // to avoid division by 0
		for (int y=0; y<yBM; y++)
			for (int x=0; x<xBM; x++)
				max = max(max, pD[y][x]);
		for (int y=0; y<yBM; y++)
			for (int x=0; x<xBM; x++)
				pD[y][x] /= max;
	}
	private float cumulativeDensity(float[][] pD, float[][] cD) {
		float volume = 0f;
		for (int y=0; y<yBM; y++) {
			for (int x=0; x<xBM; x++) {
				volume  += pD[y][x];
				cD[y][x] = volume;
			}
		}
		return volume;		
	}
	private void normalizeCDToOne(float[][] cD, float volume) {
		for (int y=0; y<yBM; y++)
			for (int x=0; x<xBM; x++)
				cD[y][x] /= volume;
	}
	private int getGrey(int red, int green, int blue) {
		if (isSum) 
			return red + green + blue;
		else
			return max(blue, max(red, green));
	}
	private void openFileAndNormalize(String path) {
		BufferedImage image;
		try {
			image = ImageIO.read(new File(path));
		} catch (IOException e) {
			// e.printStackTrace();
			genGaussian(GX, GY, GS);
			return;
		}
		yBM = image.getHeight();
		xBM = image.getWidth();
		if (yBM == 0 || xBM == 0) {
			genGaussian(GX, GY, GS);
			return;
		}
		starsPD = new float[yBM][xBM];
		if (isColor) {
			redPD   = new float[yBM][xBM];
			greenPD = new float[yBM][xBM];
			bluePD  = new float[yBM][xBM];
		}  else if (isSharp) {
			sharpPD = new float[yBM][xBM];
		}
		
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = image.getRGB(x, y);
				int red   = (pixel >> 16) & 0xff;
				int green = (pixel >> 8) & 0xff;
				int blue  = (pixel) & 0xff;
				int grey  = getGrey(red, green, blue);
				
				starsPD[y][x] = grey;
				if (isColor) {
					redPD  [y][x] = red;
					greenPD[y][x] = green;
					bluePD [y][x] = blue;
				} else if (isSharp) {
					sharpPD  [y][x] = grey;
				}
			}
		}
		normalizeToOne(starsPD);
		if (isColor) {
			normalizeToOne(redPD);
			normalizeToOne(greenPD);
			normalizeToOne(bluePD);
		} else if (isSharp) {
			normalizeToOne(sharpPD);
		}
	}
	private void setRandom(float[][] cD, Point.Float pt) {
		float source = rand.nextFloat();
		int col = xBM-1;
		int row;
		for (row=0; row<yBM; row++) {
			if(source < cD[row][col])
				break;
		}
		if (row == yBM) // source = 1.0
			row--;
		for (col=0; col<xBM; col++) {
			if(source < cD[row][col])
				break;
		} 
		if (col == xBM) // source = 1.0
			col--;
		float x = col + rand.nextFloat();
		float y = row + rand.nextFloat();
		
		pt.x = offset + x * xMult;
		pt.y = offset + y * yMult;
	}
	private boolean initMultiple() {
		float[][] greyCD = new float[yBM][xBM];
		float vol = cumulativeDensity(starsPD, greyCD);
		if (vol == 0)
			return false; // Not a valid Multiple
		
		// Locate black lines
		List<Integer> blakLines = new ArrayList<>();
		blakLines.add(-1); // No black lines required at start
		int lastCol = xBM-1;
		float lastSum = 0f;
		for (int row=0; row<yBM; row++) {
			float cs = greyCD[row][lastCol];
			if (cs == lastSum)
				blakLines.add(row); // Found a new black line
			lastSum = cs;
		}
		blakLines.add(yBM);  // No black lines required at end

		// Locate blocks
		List<Integer> startList = new ArrayList<>();
		List<Integer> stopList  = new ArrayList<>();
		Integer lastBL = -1;
		for (Integer bl : blakLines) {
			int diff = bl - lastBL;
			if (diff > 1) { // end of block
				startList.add(lastBL+1);
				stopList.add(bl);
			}
			lastBL = bl;
		}
		int blockCount = startList.size();

		// StarMap
		int starsStart = startList.get(0);
		yBM = stopList.get(0) - starsStart;
		
		starsCD = new float[yBM][xBM];
		starsPD = new float[yBM][xBM];
		for (int row=0; row<yBM; row++) // full copy to be able to retrieve greenPD as Mask
			for (int col=0; col<xBM; col++)
				starsPD[row][col] = greenPD[row+starsStart][col];

		normalizeToOne(starsPD);
		processOption2(starsPD);
		volume = cumulativeDensity(starsPD, starsCD);
		if (volume == 0)
			return false; // Not a valid Multiple
		normalizeCDToOne(starsCD, volume);
		alienCD	= new float[1][yBM][xBM];
		alienCD[0] = starsCD;
		userCD	 = starsCD;
		orionCD	= starsCD;
		nebulaeCD  = starsCD;

		// Nebulae Map
		int mapId = 1;
		if (blockCount == mapId)
			return true; // Incomplete, but valid Multiple
		nebulaeCD = getSubMap(startList.get(mapId), stopList.get(mapId), starsStart);
		
		// Orion Map
		mapId += 1;
		if (blockCount == mapId)
			return true; // Incomplete, but valid Multiple
		orionCD = getSubMap(startList.get(mapId), stopList.get(mapId), starsStart);
		
		// User Map
		sharpNb += maskNb ; // To be more inside stars
		mapId += 1;
		if (blockCount == mapId)
			return true; // Incomplete, but valid Multiple
		userCD = getSubMap(startList.get(mapId), stopList.get(mapId), starsStart);

		// alien Map
		mapId += 1;
		if (blockCount == mapId)
			return true; // Incomplete, but valid Multiple
		alienSize = blockCount-mapId;
		alienCD = new float[alienSize][yBM][xBM];
		for (int i=0; i<alienSize; i++)
			alienCD[i] = getSubMap(startList.get(mapId+i), stopList.get(mapId), starsStart);

		return true;
	}
	private float[][] getSubMap(int start, int stop, int starsStart) {
		float[][] mapCD = new float[yBM][xBM];
		float[][] mapPD = new float[yBM][xBM];

		for (int i=0; i<yBM; i++)
			mapPD[i] = redPD[i+start];
		if (isMask) {
			int rowMax = min(yBM, stop-start);
			for (int row=0; row<rowMax; row++)
				for (int col=0; col<xBM; col++)
					mapPD[row][col] *= greenPD[row+starsStart][col];
		}
		normalizeToOne(mapPD);
		processOption2(mapPD);
		float mapVol = cumulativeDensity(mapPD, mapCD);
		if (mapVol == 0) // Empty ==> star map
			mapCD = starsCD;
		else
			normalizeCDToOne(mapCD, mapVol);
		return mapCD;
	}
	private float[][] alienCD(int id) {
		int idx = Math.floorMod(id, alienSize);
		// System.out.println("alienCD(int id): alienSize= " + alienSize + "  id= " + id + " idx= " + idx);
		return alienCD[idx];
	}
	private float[][] alienCD() { return alienCD[0]; }
	private void processOption2(float[][] pD) {
		if (isInverted)
			invertPD(pD);
		if (isSharp)
			for (int i=0; i<sharpNb; i++)
				sharpenPD(pD);
	}
	private void setOption1() {
		String option1 = opts.selectedGalaxyShapeOption1();
		Option1Enum opt1 = Option1Enum.values()[0];
		for (Option1Enum o1 : Option1Enum.values())
			if(option1.endsWith(o1.toString())) {
				opt1 = o1;
				break;
			}
		switch (opt1) {
			case SETUP_BITMAP_GREY_MAX:
				isSum		= false;
				isInverted	= false;
				isColor		= false;
				isAdvanced	= false;
				isMask		= false;
				maskNb		= 0;
				break;
			case SETUP_BITMAP_GREY_SUM:
				isSum		= true;
				isInverted	= false;
				isColor		= false;
				isAdvanced	= false;
				isMask		= false;
				maskNb		= 0;
				break;
			case SETUP_BITMAP_GREY_INVERSE:
				isSum		= false;
				isInverted	= true;
				isColor		= false;
				isAdvanced	= false;
				isMask		= false;
				maskNb		= 0;
				break;
			case SETUP_BITMAP_COLOR:
				isSum		= false;
				isInverted	= false;
				isColor		= true;
				isAdvanced	= false;
				isMask		= false;
				maskNb		= 0;
				break;
			case SETUP_BITMAP_ADVANCED:
				isSum		= false;
				isInverted	= false;
				isColor		= true;
				isAdvanced	= true;
				isMask		= false;
				maskNb		= 0;
				break;
			case SETUP_BITMAP_ADVANCED_MASK:
				isSum	 	= false;
				isInverted	= false;
				isColor		= true;
				isAdvanced	= true;
				isMask		= true;
				maskNb		= 1;
				break;
			case SETUP_BITMAP_ADVANCED_MASK2:
				isSum		= false;
				isInverted	= false;
				isColor		= true;
				isAdvanced	= true;
				isMask		= true;
				maskNb		= 3;
				break;
		}
	}
	private void setOption2() {
		String option2 = opts.selectedGalaxyShapeOption2();
		Option2Enum opt2 = Option2Enum.values()[0];
		for (Option2Enum o2 : Option2Enum.values())
			if(option2.endsWith(o2.toString())) {
				opt2 = o2;
				break;
			}
		switch (opt2) {
			case SETUP_BITMAP_NORMAL:
				isSharp = false;
			   	sharpNb = 0;
				break;
			case SETUP_BITMAP_SHARP1:
			   	isSharp = true;
			   	sharpNb = 1;
				break;
			case SETUP_BITMAP_SHARP2:
			   	isSharp = true;
			   	sharpNb = 2;
				break;
			case SETUP_BITMAP_SHARP3:
			   	isSharp = true;
			   	sharpNb = 3;
				break;
			case SETUP_BITMAP_SHARP4:
			   	isSharp = true;
			   	sharpNb = 4;
				break;
		}
	}
	private void postSingleInit() {
		aspectRatio = (float) xBM / yBM;
		float volumeFactor = 1/volume *xBM*yBM;
		densityFactor = (float) Math.pow(volumeFactor, 1.0/3.0);
		densityFactor = (float) (volumeFactor/3.0); // TODO BR: Optimize
		adjustDensity = sqrt(densityFactor);
		
	}
	@Override protected void singleInit(boolean full) {
		super.singleInit(full);
		// System.out.println("========== GalaxyBitmapShape.singleInit()");
		alienSize = 1;
		setOption1();
		setOption2();

		// Get bitmap (Normalized to One)
		String option3 = UserPreferences.shapeOption3.get();
		if(option3==null || option3.equals(""))
			genGaussian(GX, GY, GS);
		else
			openFileAndNormalize(option3);

		if (isAdvanced) {
			if (initMultiple()) {
				postSingleInit();
				return;
			}
			else // Failed ==> default galaxy
				genGaussian(GX, GY, GS);
		}
		processOption2(starsPD);
		if (isSharp)
			sharpNb+=1;
		if (isColor) {
			processOption2(redPD);
			processOption2(greenPD);
			processOption2(bluePD);
		} else if (isSharp) {
			processOption2(sharpPD);			
		}

		// Normalize and validate bitmap
		starsCD = new float[yBM][xBM];
		volume = cumulativeDensity(starsPD, starsCD);
		if (volume == 0) { // Empty ==> default shape
			genGaussian(GX, GY, GS);
			if (isSharp)
				processOption2(sharpPD);			
			volume = cumulativeDensity(starsPD, starsCD);
		}
		normalizeCDToOne(starsCD, volume);
		alienCD	= new float[1][yBM][xBM];
		if (isSharp) {
			userCD = new float[yBM][xBM];
			float v = cumulativeDensity(sharpPD, userCD);
			normalizeCDToOne(userCD, v);
			alienCD[0] = userCD;
			orionCD	= userCD;
			nebulaeCD  = userCD;
		} else {
			alienCD[0] = starsCD;
			userCD	 = starsCD;
			orionCD	= starsCD;
			nebulaeCD  = starsCD;
		}
		if (isColor) {
			alienCD[0] = new float[yBM][xBM];
			float redVol = cumulativeDensity(redPD, alienCD());
			if (redVol == 0) { // Empty ==> star map
				redPD	  = starsPD;
				alienCD[0] = starsCD;
			} else
				normalizeCDToOne(alienCD(), redVol);

			userCD = new float[yBM][xBM];
			float greenVol = cumulativeDensity(greenPD, userCD);
			if (greenVol == 0) { // Empty ==> star map
				greenPD = starsPD;
				userCD  = starsCD;
			} else
				normalizeCDToOne(userCD, greenVol);

			orionCD = new float[yBM][xBM];
			float blueVol = cumulativeDensity(bluePD, orionCD);
			if (blueVol == 0) { // Empty ==> star map
				bluePD  = starsPD;
				orionCD = starsCD;
			} else
				normalizeCDToOne(orionCD, blueVol);
		}

		postSingleInit();
	}
	@Override public void clean() {
		starsPD  = null;
		redPD   = null;
		greenPD = null;
		bluePD  = null;
		starsCD  = nebulaeCD;
	}
	@Override public List<String> options1()  { return options1; }
	@Override public List<String> options2()  { return options2; }
	@Override public String defaultOption1()  { return options1.get(0); }
	@Override public String defaultOption2()  { return options2.get(0); }
	@Override public void init(int n) {
		super.init(n);
		// reset w/h vars since aspect ratio may have changed
		initWidthHeight();
		offset = galaxyEdgeBuffer();
		xMult  = (float) width/xBM;
		yMult  = (float) height/yBM;
	}
	@Override public float maxScaleAdj()	{ return 1.1f; }
	@Override protected int galaxyWidthLY() { 
		return (int) (Math.sqrt(opts.numberStarSystems()*adjustDensity*adjustedSizeFactor()*aspectRatio));
	}
	@Override protected int galaxyHeightLY() { 
		return (int) (Math.sqrt(opts.numberStarSystems()*adjustDensity*adjustedSizeFactor()/aspectRatio));
	}
	@Override public void setRandom(Point.Float pt) { setRandom(starsCD, pt); }
	// modnar: add possibility for specific placement of homeworld/orion locations
	@Override public void setSpecific(Point.Float pt) {
		if (indexWorld == 0) { // orion
			setRandom(orionCD, pt);
			return;
		}
		if (empSystems.size() == 0) { // Player homeworld
			setRandom(userCD, pt);
			return;
		}
		// Aliens homeworlds
   		setRandom(alienCD(empSystems.size()-1), pt);
	}
	@Override public boolean valid(float x, float y) {
		if (x<0)		  return false;
		if (y<0)		  return false;
		if (x>fullWidth)  return false;
		if (y>fullHeight) return false;
		return true;
	}
	@Override protected float sizeFactor(String size) { return settingsFactor(0.8f); }
}
