package intermidia;

import java.io.File;
import java.io.FileWriter;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;
import org.openimaj.video.xuggle.XuggleVideo;

import TVSSUnits.Shot;
import TVSSUnits.ShotList;
import TVSSUtils.ShotReader;

public class SimpleKeyframeDetector 
{
    public static void main( String[] args ) throws Exception
    {
		XuggleVideo source = new XuggleVideo(new File(args[0]));
		System.out.println("Reading shots.");
		ShotList shotList = ShotReader.readFromCSV(source, args[1]);	
		System.out.println("Processing shots.");
		FileWriter keyframeWriter = new FileWriter(args[2]);
		int shotIndex = 0;
		for(Shot shot: shotList.getList())	
		{
			long firstFrame = shot.getStartBoundary().getTimecode().getFrameNumber();
			long lastFrame = shot.getEndBoundary().getTimecode().getFrameNumber();
			long frameQty = (lastFrame - firstFrame + 1);			

			//Calculate a color histogram of each frame in the shot
			MultidimensionalHistogram histogram[] = new MultidimensionalHistogram[(int) frameQty];			
			HistogramModel histogramModel = new HistogramModel(4,4,4);
						
			for(int i = 0; i < frameQty; i++)
			{
				source.setCurrentFrameIndex(firstFrame + i); 
				histogramModel.estimateModel(source.getCurrentFrame());
				histogram[i] = histogramModel.histogram.clone();				
			}
			
			
			//Calculate mean distance from a frame to all other in same shot
			double meanDistance[] = new double[(int) frameQty];
			for(int i = 0; i < frameQty; i++)
			{
				meanDistance[i] = 0;
				for(int j = 0; j < frameQty; j++)
				{
					meanDistance[i] += histogram[i].compare(histogram[j], DoubleFVComparison.EUCLIDEAN);
				}
				meanDistance[i] /= frameQty;
			}
			
			int minDistanceIndex = 0;
			double minDistance = meanDistance[minDistanceIndex];			
			for(int i = 1; i < frameQty; i++)
			{
				if(meanDistance[i] < minDistance)
				{
					minDistanceIndex = i;
					minDistance = meanDistance[minDistanceIndex];
				}
			}
			keyframeWriter.write(shotIndex + "\t" + (firstFrame + minDistanceIndex) + "\n");
			System.out.println("Shot " + shotIndex + ": " + shot.getStartBoundary().getTimecode().getFrameNumber() + " - " +  shot.getEndBoundary().getTimecode().getFrameNumber() +
					 " | Keyframe @ " + (firstFrame + minDistanceIndex));
			shotIndex++;
		}
		keyframeWriter.close();
		source.close();
    }
}
 