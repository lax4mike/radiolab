package radiolab;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RadiolabScraper {
	
	public static void main(String[] args) {
		
		RadiolabScraper scaper = new RadiolabScraper();
		scaper.run();
		
	}
	
	public void run() {
		
		int[] getTheseSeason = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
		
		
		for (int getThis : getTheseSeason) {
			
			this.getRadioLabSeason(getThis);
			
		}
		
	}
	
	protected void getRadioLabSeason(int season){
		
		try {
			
			int i = 0;
			Document doc = Jsoup.connect("http://www.radiolab.org/archive/").get();
			
			Elements seasons = doc.select(".interior-lead");
						
			for (Element s : seasons) {
				
				String seasonString = s.select("h1").first().text();
				int seasonNum = Integer.parseInt(seasonString.replaceAll("\\D", ""));
				
				if (seasonNum != season){
					continue;
				}
						
				Elements episodes = s.select("#radiolab-archive-list li");
				
				// System.out.println(season);
				
				for (Element e : episodes) {

					String href = e.select(".read-more").attr("href");
					String title= e.select("h4").first().text();
					
//					System.out.println(title);
//					System.out.println(href);
					
					this.getRadioLabEpisode(href);
					
					i++;
				}
				
			}
//			System.out.println(i + " episodes");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void getRadioLabEpisode(String href) {
		
		try {
			Document doc = Jsoup.connect(href).get();
			
			String se = doc.select(".seanum-epnum").text();
			String[] seArray = se.split("\\|");

			// build filename
			String season = seArray[0].replaceAll("[^0-9]", "");
			String episode = seArray[1].replaceAll("[^0-9]", "");
			String title = doc.select(".story-headergroup h2.title").text();
			
			String filepath = "Downloaded";
			filepath += File.separator;
			filepath += "Season " + season;
			filepath += File.separator;			
			
			
			String filename = season;
			filename += String.format("%02d", Integer.parseInt(episode));
			filename += " - ";
			filename += title;
			filename += ".mp3";
			
			// escape chars for windows filename:
			
			filename = this.getValidFileName(filename);
			
			// download file
			String xmlDocUrl = doc.select(".inline_audioplayer_wrapper").first().select(".player_element").first().attr("data-url");
			Document xmlDoc = Jsoup.connect(xmlDocUrl).get();
			String downloadLink = xmlDoc.select("location").text();
			
			URL downloadUrl = new URL(downloadLink);
			File saveFile = new File(filepath+filename);
			
			System.out.println("Downloading " + filename + " ...");	
			org.apache.commons.io.FileUtils.copyURLToFile(downloadUrl, saveFile);
				
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	// not robust, but works for this.
	private String getValidFileName(String fileName) {
	    String newFileName = fileName.replaceAll("\\?|\\!", "");
	    if(newFileName.length()==0)
	        throw new IllegalStateException(
	                "File Name " + fileName + " results in a empty fileName!");
	    return newFileName;
	}

}
