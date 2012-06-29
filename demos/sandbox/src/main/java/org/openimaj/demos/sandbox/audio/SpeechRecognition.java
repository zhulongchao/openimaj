/**
 * 
 */
package org.openimaj.demos.sandbox.audio;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import org.openimaj.audio.AudioFormat;
import org.openimaj.audio.AudioPlayer;
import org.openimaj.audio.conversion.SampleRateConverter;
import org.openimaj.audio.conversion.SampleRateConverter.SampleRateConversionAlgorithm;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.typography.FontStyle;
import org.openimaj.image.typography.general.GeneralFont;
import org.openimaj.image.typography.general.GeneralFontRenderer;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.video.xuggle.XuggleAudio;
import org.openimaj.vis.audio.AudioWaveformPlotter;

import com.sun.org.apache.xerces.internal.util.URI;

/**
 * Basic Sphinx demo (from their webpage). Uses the OpenIMAJ audio file data
 * source to link OpenIMAJ audio engine to Sphinx.
 * 
 * @author David Dupplaw (dpd@ecs.soton.ac.uk)
 * 
 * @created 23 May 2012
 */
public class SpeechRecognition
{
	/**
	 * @param args
	 * @throws PropertyException
	 * @throws IOException
	 * @throws InstantiationException
	 */
	public static void main( String[] args ) throws IOException,
	        PropertyException, InstantiationException
	{
		URL configFile = SpeechRecognition.class
		        .getResource( "/org/openimaj/demos/sandbox/audio/sphinx-config-hub4.xml" );

		// Check the configuration file exists
		if( configFile == null )
		{
			System.err.println( "Cannot find config file" );
			System.exit( 1 );
		}

		// Get the audio file input
		// URL audioFileURL = new URL( "http://www.moviewavs.com/0058349934/WAVS/Movies/Juno/experimenting.wav" );
		File audioFileURL = new File( "videoplayback.3gp" );

		// Check whether the audio file exists
		if( audioFileURL != null )
		{
			try
			{
				List<Rectangle> boundingBoxes = new ArrayList<Rectangle>();
				
				System.out.println( audioFileURL );
				
				// Get a display of the audio waveform
				XuggleAudio xa = new XuggleAudio( audioFileURL );
				AudioWaveformPlotter awp = new AudioWaveformPlotter();
				MBFImage awi = awp.plotAudioWaveformImage( xa, 1000, 300,
				        new Float[]
				        { 0f, 0f, 0f, 1f }, new Float[]
				        { 1f, 1f, 1f, 1f } );

				System.out.println( awp.millisecondsInView );

				MBFImage img = new MBFImage( 1000, 400, 3 );
				img.drawImage( awi, 0, 0 );
				DisplayUtilities.displayName( img, "waveform" );

				// Load the configuration
				ConfigurationManager cm = new ConfigurationManager( configFile );

				// Allocate the recognizer
				System.out.println( "Loading..." );
				Recognizer recognizer = (Recognizer)cm.lookup( "recognizer" );
				recognizer.allocate();

				// Configure the audio input for the recognizer
				OpenIMAJAudioFileDataSource dataSource = (OpenIMAJAudioFileDataSource)cm
				        .lookup( "audioFileDataSource" );
				XuggleAudio xa2 = new XuggleAudio( audioFileURL );
				SampleRateConverter src = new SampleRateConverter( xa2, 
						SampleRateConversionAlgorithm.LINEAR_INTERPOLATION,
						new AudioFormat( xa2.getFormat().getNBits(), 16,
								xa2.getFormat().getNumChannels() ) );
				dataSource.setAudioStream( src );

				GeneralFont font = new GeneralFont("Courier", Font.PLAIN, 24);
				FontStyle<GeneralFont, Float[]> fontStyle = font.createStyle( awi.createRenderer() );
				
				// Start recognising words from the audio file
				Pattern p = Pattern.compile( "([A-Za-z0-9'_]+)\\(([0-9.]+),([0-9.]+)\\)" );
				Result result = null;
				StringBuffer sb = new StringBuffer();
				while( (result = recognizer.recognize()) != null )
				{
					String resultText = result.getTimedBestResult( false, true );
					System.out.println( resultText );

					Matcher matcher = p.matcher( resultText );
					while( matcher.find() )
					{
						System.out.println( "Word:  " + matcher.group( 1 ) );
						System.out.println( "Start: " + matcher.group( 2 ) );
						System.out.println( "End:   " + matcher.group( 3 ) );

						// Parse the word and timings from the result
						String word = matcher.group(1);
						double s = Double.parseDouble( matcher.group(2) ) * 1000;
						double e = Double.parseDouble( matcher.group(3) ) * 1000;
						sb.append( word+" " );

						// Get the bounds of the word polygon
						Rectangle bounds = font.getRenderer( 
								awi.createRenderer() ).getBounds( 
										word, fontStyle );

						// Determine the pixel coordinate of the start and end times
						int startX = (int)(s/awp.millisecondsInView*1000);
						int endX   = (int)(e/awp.millisecondsInView*1000);
						
						// Draw bars showing the range of the word
						img.drawLine( startX, 320, endX, 320, RGBColour.YELLOW );
						img.drawLine( startX, 318, startX, 322, RGBColour.GREEN );
						img.drawLine( endX, 318, endX, 322, RGBColour.RED );
						
						int y = 350;
						bounds.translate( startX, y );
						boolean noIntersection = true;
						do
						{
							noIntersection = true;
							for( Rectangle r : boundingBoxes )
								if( r.isOverlapping( bounds ) )
								{ noIntersection = false; break; }
							
							if( !noIntersection )
								bounds.translate( 0, bounds.height );
						} while( !noIntersection );
						y = (int)bounds.y;
							
						// Draw the word
						img.drawLine( startX, 322, startX, (int)(y+bounds.height), 
								new Float[]{0.4f,0.4f,0.4f} );
						img.drawLine( startX, (int)(y+bounds.height), startX+8,
								(int)(y+bounds.height), new Float[]{0.4f,0.4f,0.4f} );
						img.drawText( word, startX, y, font, 1, RGBColour.WHITE  );
						
						// Store the bounding box
						boundingBoxes.add( bounds );
					}
				}

				DisplayUtilities.displayName( img, "waveform" );
				System.out.println( "=======================================" );
				System.out.println( "Text: \n"+sb.toString() );
				System.out.println( "=======================================" );

				xa = new XuggleAudio( audioFileURL );
				AudioPlayer ap = AudioPlayer.createAudioPlayer( 
						xa, "Line 1/2 (M-Audio Delta 44)" );
				ap.run();
			}
			catch( NumberFormatException e )
			{
				e.printStackTrace();
			}
			catch( IllegalStateException e )
			{
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println( "The audio file " + audioFileURL
			        + " could not be found" );
		}
	}
}
