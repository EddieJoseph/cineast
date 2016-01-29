package ch.unibas.cs.dbis.cineast.core.descriptor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import ch.unibas.cs.dbis.cineast.core.config.Config;
import ch.unibas.cs.dbis.cineast.core.data.MultiImage;
import ch.unibas.cs.dbis.cineast.core.data.MultiImageFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class EdgeImg {

	private EdgeImg() {
	}

	private static final Logger LOGGER = LogManager.getLogger();

	private static final float THRESHOLD_LOW = 0.075f, THRESHOLD_HIGH = 0.3f;

	//private static final CannyEdge<ImageUInt8, ImageSInt16> canny = FactoryEdgeDetectors.canny(2, false, true, ImageUInt8.class, ImageSInt16.class);
	
	public static MultiImage getEdgeImg(MultiImage img) {
		LOGGER.entry();

		ImageUInt8 gray = ConvertBufferedImage.convertFrom(img.getBufferedImage(), (ImageUInt8) null);
		if(!isSolidBlack(gray)){
			getCanny().process(gray, THRESHOLD_LOW, THRESHOLD_HIGH, gray);
		}

		BufferedImage bout = VisualizeBinaryData.renderBinary(gray, null);

		return LOGGER.exit(MultiImageFactory.newMultiImage(bout));
	}

	public static boolean[] getEdgePixels(MultiImage img, boolean[] out) {
		LOGGER.entry();

		if (out == null || out.length != img.getWidth() * img.getHeight()) {
			out = new boolean[img.getWidth() * img.getHeight()];
		}

		ImageUInt8 gray = ConvertBufferedImage.convertFrom(img.getBufferedImage(), (ImageUInt8) null);

		if(!isSolidBlack(gray)){
			getCanny().process(gray, THRESHOLD_LOW, THRESHOLD_HIGH, gray);
			
		}

		for (int i = 0; i < gray.data.length; ++i) {
			out[i] = (gray.data[i] != 0);
		}

		LOGGER.exit();
		return out;
	}

	public static List<Boolean> getEdgePixels(MultiImage img, List<Boolean> out) {
		LOGGER.entry();
		if (out == null) {
			out = new ArrayList<Boolean>(img.getWidth() * img.getHeight());
		} else {
			out.clear();
		}
		
		BufferedImage withBackground = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = withBackground.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.drawImage(img.getBufferedImage(), 0, 0, null);
		
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(withBackground, (ImageUInt8) null);
		if(!isSolidBlack(gray)){
			getCanny().process(gray, THRESHOLD_LOW, THRESHOLD_HIGH, gray);
		}

		for (int i = 0; i < gray.data.length; ++i) {
			out.add(gray.data[i] != 0);
		}
		LOGGER.exit();
		return out;
	}
	
	public static boolean isSolidBlack(ImageUInt8 img){
		for(byte b : img.data){
			if(b != 0){
				return false;
			}
		}
		return true;
	}
	
	//private static HashMap<Thread, CannyEdge<ImageUInt8, ImageSInt16>> cannies = new HashMap<Thread, CannyEdge<ImageUInt8,ImageSInt16>>();
	private static LoadingCache<Thread, CannyEdge<ImageUInt8, ImageSInt16>> cannies = CacheBuilder.newBuilder().maximumSize(Config.numbetOfPoolThreads() * 2)
			.expireAfterAccess(10, TimeUnit.MINUTES).build(new CacheLoader<Thread, CannyEdge<ImageUInt8, ImageSInt16>>(){

				@Override
				public CannyEdge<ImageUInt8, ImageSInt16> load(Thread arg0){
					return FactoryEdgeDetectors.canny(2, false, true, ImageUInt8.class, ImageSInt16.class);
				}});
	private static synchronized CannyEdge<ImageUInt8, ImageSInt16> getCanny(){
		Thread current = Thread.currentThread();
		try {
			return cannies.get(current);
		} catch (ExecutionException e) {
			return null; //NEVER HAPPENS
		}
	}
}
